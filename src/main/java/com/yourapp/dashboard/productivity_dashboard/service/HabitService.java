package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.config.SleepWindow;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class HabitService {

    private final HabitRepository habitRepo;
    private final HabitLogRepository logRepo;
    private final TelegramService telegramService;
    private final SleepWindow sleepWindow;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final int DEFAULT_GRACE_PERIOD = 30; // minutes

    @Autowired
    public HabitService(HabitRepository habitRepo,
                        HabitLogRepository logRepo,
                        TelegramService telegramService,
                        SleepWindow sleepWindow) {
        this.habitRepo = habitRepo;
        this.logRepo = logRepo;
        this.telegramService = telegramService;
        this.sleepWindow = sleepWindow;
    }

    public List<Habit> getAllHabits() {
        return habitRepo.findByArchivedFalse();
    }

    public List<Habit> getArchivedHabits() {
        return habitRepo.findByArchivedTrue();
    }
    
    /**
     * Get statistics about habits and their completion rates
     * @return Map containing various habit statistics
     */
    public Map<String, Object> getHabitStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Habit> habits = getAllHabits();
        
        // Basic counts
        stats.put("totalHabits", habits.size());
        stats.put("activeHabits", habits.stream().filter(h -> !h.isArchived()).count());
        stats.put("archivedHabits", habits.stream().filter(Habit::isArchived).count());
        
        // Completion stats
        List<HabitLog> recentLogs = logRepo.findTop50ByOrderByScheduledDateTimeDesc();
        long totalCompletions = recentLogs.stream().filter(log -> Boolean.TRUE.equals(log.getCompleted())).count();
        long totalMissed = recentLogs.stream().filter(log -> Boolean.TRUE.equals(log.getMissed())).count();
        long totalSkipped = recentLogs.stream().filter(log -> Boolean.TRUE.equals(log.isSkipped())).count();
        
        // Get today's date at start of day for filtering
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        // Count completions today
        long totalCompletedToday = logRepo.countByCompletedAndScheduledDateTimeBetween(true, startOfDay, endOfDay);
        
        // Get current streak (max of all active habits' current streaks)
        int currentStreak = habits.stream()
            .filter(h -> !h.isArchived())
            .mapToInt(Habit::getCurrentStreak)
            .max()
            .orElse(0);
        
        stats.put("totalCompletions", totalCompletions);
        stats.put("totalMissed", totalMissed);
        stats.put("totalSkipped", totalSkipped);
        stats.put("totalCompletedToday", totalCompletedToday);
        stats.put("currentStreak", currentStreak);
        
        // Calculate completion rate (avoid division by zero)
        double completionRate = recentLogs.isEmpty() ? 0 : 
            (double) totalCompletions / (totalCompletions + totalMissed + totalSkipped) * 100;
        stats.put("completionRate", Math.round(completionRate * 10) / 10.0);
        
        // Streak statistics
        Optional<Habit> bestStreakHabit = habits.stream()
            .filter(h -> !h.isArchived())
            .max(Comparator.comparingInt(Habit::getBestStreak));
            
        bestStreakHabit.ifPresent(habit -> {
            stats.put("bestStreak", habit.getBestStreak());
            stats.put("bestStreakHabit", habit.getName());
        });
        
        // Habits by frequency
        Map<Recurrence, Long> habitsByFrequency = habits.stream()
            .filter(h -> !h.isArchived())
            .collect(Collectors.groupingBy(Habit::getRecurrence, Collectors.counting()));
        stats.put("habitsByFrequency", habitsByFrequency);
        
        // Habits by priority
        Map<Priority, Long> habitsByPriority = habits.stream()
            .filter(h -> !h.isArchived())
            .collect(Collectors.groupingBy(Habit::getPriority, Collectors.counting()));
        stats.put("habitsByPriority", habitsByPriority);
        
        // Recent activity
        List<Map<String, Object>> recentActivity = recentLogs.stream()
            .limit(10)
            .map(log -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("habitName", log.getHabit().getName());
                activity.put("scheduledTime", log.getScheduledDateTime());
                activity.put("status", log.isCompleted() ? "Completed" : 
                                      log.getMissed() ? "Missed" : "Skipped");
                return activity;
            })
            .collect(Collectors.toList());
        stats.put("recentActivity", recentActivity);
        
        return stats;
    }

    public Optional<Habit> getHabitById(Long id) {
        return habitRepo.findById(id);
    }

    @Transactional
    public Habit createHabit(Habit habit) {
        if (habit == null) {
            throw new IllegalArgumentException("Habit cannot be null");
        }
        habit.setArchived(false);
        habit.setCurrentStreak(0);
        habit.setBestStreak(0);
        habit.setLastCompleted(null);
        habit.setCreatedAt(LocalDateTime.now());
        habit.setUpdatedAt(LocalDateTime.now());

        // Set default values if not provided
        if (habit.getGracePeriodMinutes() == null) {
            habit.setGracePeriodMinutes(DEFAULT_GRACE_PERIOD);
        }
        if (habit.getRecurrence() == null) {
            habit.setRecurrence(Recurrence.DAILY);
        }

        return habitRepo.save(habit);
    }

    @Transactional
    public HabitLog completeHabit(Long habitId, Long logId) {
        if (habitId == null || logId == null) {
            throw new IllegalArgumentException("Habit ID and Log ID must not be null");
        }

        Habit habit = habitRepo.findById(habitId)
                .orElseThrow(() -> new RuntimeException("Habit not found with id: " + habitId));

        // For hourly habits, we need to ensure we're working with a managed entity
        HabitLog log;
        if (habit.getRecurrence() == Recurrence.HOURLY) {
            // Find the log in the current persistence context
            log = logRepo.findById(logId)
                    .orElseThrow(() -> new RuntimeException("Habit log not found with id: " + logId));
            
            // Verify the log belongs to this habit
            if (!habit.getId().equals(log.getHabit().getId())) {
                throw new IllegalArgumentException("Log does not belong to the specified habit");
            }
            
            // If the log is already completed, just return it
            if (log.getCompleted()) {
                return log;
            }
            
            // Mark as completed
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledTime = log.getScheduledDateTime();
            int gracePeriod = habit.getGracePeriodMinutes() != null ?
                    habit.getGracePeriodMinutes() : DEFAULT_GRACE_PERIOD;
            boolean withinGracePeriod = now.isBefore(scheduledTime.plusMinutes(gracePeriod));
            
            log.setCompleted(true);
            log.setCompletedDateTime(now);
            log.setGracePeriodUsed(!scheduledTime.isBefore(now) || !withinGracePeriod);
            log.setCompletedInGracePeriod(withinGracePeriod);
            log.setUpdatedAt(now);
            
            // Save the log
            log = logRepo.save(log);
            
            // Update habit stats and streaks
            updateHabitStreaks(habit);
            habit.setLastCompleted(LocalDate.now());
            habit.setUpdatedAt(now);
            habitRepo.save(habit);
            
            return log;
        } else {
            // Original logic for non-hourly habits
            log = logRepo.findById(logId)
                    .orElseThrow(() -> new RuntimeException("Habit log not found with id: " + logId));

            if (!habit.getId().equals(log.getHabit().getId())) {
                throw new IllegalArgumentException("Log does not belong to the specified habit");
            }

            if (log.getCompleted()) {
                return log; // Already completed
            }

            // Check if within grace period
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledTime = log.getScheduledDateTime();
            int gracePeriod = habit.getGracePeriodMinutes() != null ?
                    habit.getGracePeriodMinutes() : DEFAULT_GRACE_PERIOD;

            boolean withinGracePeriod = now.isBefore(scheduledTime.plusMinutes(gracePeriod));

            // Mark as completed
            log.setCompleted(true);
            log.setCompletedDateTime(now);
            log.setGracePeriodUsed(!scheduledTime.isBefore(now) || !withinGracePeriod);
            log.setCompletedInGracePeriod(withinGracePeriod);
            log.setUpdatedAt(now);

            // Update habit stats and streaks
            updateHabitStreaks(habit);
            habit.setLastCompleted(LocalDate.now());
            habit.setUpdatedAt(now);
            
            return logRepo.save(log);
        }
    }


    @Transactional
    public Habit updateHabit(Habit habit) {
        if (habit == null || habit.getId() == null) {
            throw new IllegalArgumentException("Habit and its ID must not be null");
        }

        Habit existingHabit = habitRepo.findById(habit.getId())
                .orElseThrow(() -> new IllegalArgumentException("Habit not found with id: " + habit.getId()));

        // Update only the fields that are allowed to be updated
        existingHabit.setName(habit.getName());
        existingHabit.setDescription(habit.getDescription());
        existingHabit.setRecurrence(habit.getRecurrence());
        existingHabit.setGracePeriodMinutes(habit.getGracePeriodMinutes());
        existingHabit.setScheduledTime(habit.getScheduledTime());
        existingHabit.setUpdatedAt(LocalDateTime.now());

        return habitRepo.save(existingHabit);
    }

    private void logHabitCompletion(Habit habit, HabitLog log, boolean withinGracePeriod) {
        try {
            String status = withinGracePeriod ? "completed on time" : "completed late";
            String message = String.format(
                    "✅ *Habit %s*\n\n" +
                            "*%s*\n" +
                            "⏰ %s\n" +
                            "🏆 Current streak: %d days\n" +
                            "🏆 Best streak: %d days\n\n" +
                            "_Great job! Keep it up!_",
                    status,
                    habit.getName(),
                    formatDateTime(log.getScheduledDateTime()),
                    habit.getCurrentStreak(),
                    habit.getBestStreak()
            );

            telegramService.sendMessage(message);

        } catch (Exception e) {
            System.err.println("Failed to send completion notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMAT) : "";
    }

    @Transactional
    public void archiveHabit(Long habitId) {
        if (habitId == null) {
            throw new IllegalArgumentException("Habit ID cannot be null");
        }

        Habit habit = habitRepo.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found with id: " + habitId));

        habit.setArchived(true);
        habit.setUpdatedAt(LocalDateTime.now());
        habitRepo.save(habit);
    }

    @Transactional
    public void unarchiveHabit(Long habitId) {
        if (habitId == null) {
            throw new IllegalArgumentException("Habit ID cannot be null");
        }

        Habit habit = habitRepo.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found with id: " + habitId));

        habit.setArchived(false);
        habit.setUpdatedAt(LocalDateTime.now());
        habitRepo.save(habit);
    }

    /**
     * Generates logs for a habit based on its recurrence pattern
     * Handles all recurrence types with support for grace periods and missed habit tracking
     */
    public void generateTodayLogsForHabit(Habit habit) {
        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of(habit.getTimeZone());
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId);

        // Check if we should generate multiple daily occurrences
        if (habit.getRecurrence() == Recurrence.DAILY && habit.isAllowMultipleDaily() &&
                !habit.getDailyReminderTimes().isEmpty()) {
            generateMultipleDailyLogs(habit, zonedNow);
            return;
        }

        // For other recurrence types or single daily occurrence
        LocalDateTime nextScheduledTime = calculateNextScheduledTime(habit, zonedNow);

        if (nextScheduledTime != null) {
            // Check if we need to mark any past occurrences as missed
            checkAndMarkMissedHabits(habit, zonedNow);

            // Create the new log if it doesn't exist
            createHabitLogIfNotExists(habit, nextScheduledTime, zoneId);
        }
    }

    /**
     * Generates logs for multiple daily occurrences of a habit
     */
    private void generateMultipleDailyLogs(Habit habit, ZonedDateTime zonedNow) {
        ZoneId zoneId = ZoneId.of(habit.getTimeZone());
        LocalDate today = zonedNow.toLocalDate();

        // Check and mark any missed occurrences from previous times today
        for (LocalTime reminderTime : habit.getDailyReminderTimes()) {
            LocalDateTime scheduledTime = LocalDateTime.of(today, reminderTime);

            // Skip if this time has already passed today and we're not in grace period
            if (scheduledTime.isBefore(zonedNow.toLocalDateTime())) {
                // Check if this was missed
                if (!isLogExistsForTime(habit, scheduledTime) &&
                        !isWithinGracePeriod(scheduledTime, zonedNow.toLocalDateTime(), habit.getGracePeriodMinutes())) {
                    markHabitAsMissed(habit, scheduledTime);
                }
                continue;
            }

            // Create log for future or current (within grace period) times
            createHabitLogIfNotExists(habit, scheduledTime, zoneId);
        }
    }

    /**
     * Calculates the next scheduled time for a habit based on its recurrence pattern
     */
    private LocalDateTime calculateNextScheduledTime(Habit habit, ZonedDateTime zonedNow) {
        LocalTime scheduledTime = habit.getScheduledTime();
        LocalDateTime now = zonedNow.toLocalDateTime();
        LocalDateTime nextScheduledTime = null;

        switch (habit.getRecurrence()) {
            case HOURLY:
                nextScheduledTime = calculateNextHourlyTime(habit, now);
                break;

            case DAILY:
                nextScheduledTime = calculateNextDailyTime(habit, now);
                break;

            case WEEKLY:
                nextScheduledTime = calculateNextWeeklyTime(habit, now);
                break;

            case MONTHLY:
                nextScheduledTime = calculateNextMonthlyTime(habit, now);
                break;

            case YEARLY:
                nextScheduledTime = calculateNextYearlyTime(habit, now);
                break;
        }

        return nextScheduledTime;
    }

    private LocalDateTime calculateNextHourlyTime(Habit habit, LocalDateTime now) {
        LocalDateTime nextTime = now.truncatedTo(ChronoUnit.HOURS)
                .plusHours(1)
                .withMinute(habit.getScheduledTime().getMinute())
                .withSecond(0)
                .withNano(0);

        // Skip sleep hours
        while (isWithinSleepHours(nextTime.toLocalTime())) {
            nextTime = nextTime.plusHours(1);
        }

        return nextTime;
    }

    private LocalDateTime calculateNextDailyTime(Habit habit, LocalDateTime now) {
        LocalDateTime nextTime = now.toLocalDate().atTime(habit.getScheduledTime());

        // If the scheduled time has passed today, move to tomorrow
        if (nextTime.isBefore(now)) {
            nextTime = nextTime.plusDays(1);
        }

        // Skip sleep hours by moving to after sleep end
        if (isWithinSleepHours(nextTime.toLocalTime())) {
            nextTime = nextTime.toLocalDate()
                    .atTime(sleepWindow.getSleepEnd())
                    .plusMinutes(5); // Add buffer after sleep end
        }

        return nextTime;
    }

    private LocalDateTime calculateNextWeeklyTime(Habit habit, LocalDateTime now) {
        DayOfWeek targetDay = habit.getWeeklyDay() != null ?
                habit.getWeeklyDay() : now.getDayOfWeek();

        LocalDateTime nextTime = now.with(TemporalAdjusters.nextOrSame(targetDay))
                .with(habit.getScheduledTime());

        // If the time has passed on the target day, move to next week
        if (nextTime.isBefore(now)) {
            nextTime = nextTime.plusWeeks(1);
        }

        return nextTime;
    }

    private LocalDateTime calculateNextMonthlyTime(Habit habit, LocalDateTime now) {
        int dayOfMonth = habit.getMonthlyDay() != null ?
                Math.min(habit.getMonthlyDay(), YearMonth.from(now).lengthOfMonth()) : 1;

        LocalDate nextDate = now.toLocalDate().withDayOfMonth(dayOfMonth);

        // If we can't set to the exact day (e.g., Feb 30), use the last day of the month
        if (nextDate.getDayOfMonth() != dayOfMonth) {
            nextDate = nextDate.withDayOfMonth(nextDate.lengthOfMonth());
        }

        LocalDateTime nextTime = nextDate.atTime(habit.getScheduledTime());

        // If the time has passed this month, move to next month
        if (nextTime.isBefore(now)) {
            nextDate = now.toLocalDate()
                    .plusMonths(1)
                    .withDayOfMonth(Math.min(dayOfMonth, YearMonth.from(now.plusMonths(1)).lengthOfMonth()));
            nextTime = nextDate.atTime(habit.getScheduledTime());
        }

        return nextTime;
    }

    private LocalDateTime calculateNextYearlyTime(Habit habit, LocalDateTime now) {
        int month = habit.getYearlyMonth() != null ? habit.getYearlyMonth() : 1;
        int day = habit.getYearlyDay() != null ? habit.getYearlyDay() : 1;

        YearMonth yearMonth = YearMonth.of(now.getYear(), month);
        day = Math.min(day, yearMonth.lengthOfMonth());

        LocalDate nextDate = LocalDate.of(now.getYear(), month, day);
        LocalDateTime nextTime = nextDate.atTime(habit.getScheduledTime());

        // If the time has passed this year, move to next year
        if (nextTime.isBefore(now)) {
            yearMonth = YearMonth.of(now.getYear() + 1, month);
            day = Math.min(day, yearMonth.lengthOfMonth());
            nextDate = LocalDate.of(now.getYear() + 1, month, day);
            nextTime = nextDate.atTime(habit.getScheduledTime());
        }

        return nextTime;
    }


    /**
     * Checks if a log exists for the given habit and scheduled time
     */
    public boolean isLogExistsForTime(Habit habit, LocalDateTime scheduledTime) {
        return logRepo.existsByHabitAndScheduledDateTime(habit, scheduledTime);
    }

    /**
     * Checks if the current time is within the grace period after the scheduled time
     */
    private boolean isWithinGracePeriod(LocalDateTime scheduledTime, LocalDateTime currentTime, Integer gracePeriodMinutes) {
        if (scheduledTime == null || currentTime == null) {
            return false;
        }
        int graceMinutes = gracePeriodMinutes != null ? gracePeriodMinutes : DEFAULT_GRACE_PERIOD;
        return !currentTime.isAfter(scheduledTime.plusMinutes(graceMinutes));
    }

    /**
     * Creates a new habit log if one doesn't already exist for the given time
     */
    public void createHabitLogIfNotExists(Habit habit, LocalDateTime scheduledTime, ZoneId zoneId) {
        if (!isLogExistsForTime(habit, scheduledTime)) {
            HabitLog log = new HabitLog();
            log.setHabit(habit);
            log.setScheduledDateTime(scheduledTime);
            log.setCompleted(false);
            log.setMissed(false);
            log.setCreatedAt(LocalDateTime.now(zoneId));
            log.setUpdatedAt(LocalDateTime.now(zoneId));
            logRepo.save(log);
        }
    }

    /**
     * Checks for and marks any missed habits
     */
    public void checkAndMarkMissedHabits(Habit habit, ZonedDateTime zonedNow) {
        LocalDateTime now = zonedNow.toLocalDateTime();
        LocalDateTime lastCheckTime = habit.getLastScheduled() != null ?
                habit.getLastScheduled() : now.minusDays(1);

        // Don't check too frequently to avoid performance issues
        if (Duration.between(lastCheckTime, now).toMinutes() < 30) {
            return;
        }

        // Update last check time
        habit.setLastScheduled(now);
        habitRepo.save(habit);

        // Find any logs that should have been completed but weren't
        List<HabitLog> pendingLogs = logRepo.findByHabitAndCompletedFalseAndMissedFalseAndScheduledDateTimeBefore(
                habit, now.minusMinutes(habit.getGracePeriodMinutes()));

        for (HabitLog log : pendingLogs) {
            // Skip if this is a future occurrence
            if (log.getScheduledDateTime().isAfter(now)) {
                continue;
            }

            // Mark as missed
            markHabitAsMissed(habit, log.getScheduledDateTime());
        }
    }

    /**
     * Marks a specific habit occurrence as missed
     */
    public void markHabitAsMissed(Habit habit, LocalDateTime scheduledTime) {
        HabitLog log = logRepo.findByHabitAndScheduledDateTime(habit, scheduledTime)
                .orElseGet(() -> {
                    HabitLog newLog = new HabitLog();
                    newLog.setHabit(habit);
                    newLog.setScheduledDateTime(scheduledTime);
                    newLog.setTimeZone(habit.getTimeZone());
                    return newLog;
                });

        if (!log.getCompleted() && !log.getMissed()) {
            log.setMissed(true);
            log.setMissedDateTime(LocalDateTime.now());
            logRepo.save(log);

            // Update habit's missed count
            habit.setMissedCount(habit.getMissedCount() + 1);
            habitRepo.save(habit);
        }
    }

    /**
     * /**
     * Checks if a time is within the grace period
     */
    private boolean isWithinGracePeriod(LocalDateTime scheduledTime, LocalDateTime currentTime, int gracePeriodMinutes) {
        return scheduledTime.plusMinutes(gracePeriodMinutes).isAfter(currentTime);
    }

    /**
     * Checks if a time is within sleep hours
     */
    private boolean isWithinSleepHours(LocalTime time) {
        return SleepWindow.isAsleep(time, sleepWindow.getSleepStart(), sleepWindow.getSleepEnd());
    }

    /**
     * Get a habit by ID (alias for getHabitById for backward compatibility)
     */
    public Optional<Habit> getHabit(Long id) {
        return getHabitById(id);
    }
    
    /**
     * Get today's active habits (alias for getTodaysHabits for backward compatibility)
     */
    public List<Habit> getTodayHabits() {
        return getTodaysHabits();
    }
    
    /**
     * Check if a habit was completed today
     */
    public boolean isCompletedToday(Habit habit) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        return !logRepo.findByHabitAndCompletedTrueAndScheduledDateTimeBetween(
            habit, startOfDay, endOfDay).isEmpty();
    }
    
    /**
     * Get upcoming logs for a habit
     */
    public List<HabitLog> getUpcomingLogs(Habit habit) {
        LocalDateTime now = LocalDateTime.now();
        List<HabitLog> upcomingLogs = new ArrayList<>();
        
        if (habit.getRecurrence() == Recurrence.HOURLY) {
            // For hourly habits, generate the next 24 hours of logs
            LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endDate = currentHour.plusDays(1);
            
            // Get existing logs for the next 24 hours
            List<HabitLog> existingLogs = logRepo.findByHabitAndScheduledDateTimeBetweenOrderByScheduledDateTimeAsc(
                habit, currentHour, endDate);
            
            // Generate hourly logs for the next 24 hours
            for (int i = 0; i < 24; i++) {
                LocalDateTime scheduledTime = currentHour.plusHours(i);
                
                // Check if we already have a log for this hour
                boolean logExists = existingLogs.stream()
                    .anyMatch(log -> log.getScheduledDateTime().equals(scheduledTime));
                
                if (!logExists) {
                    // Create a new log for this hour
                    HabitLog log = new HabitLog();
                    log.setHabit(habit);
                    log.setScheduledDateTime(scheduledTime);
                    log.setCompleted(false);
                    upcomingLogs.add(log);
                } else {
                    // Use the existing log
                    HabitLog existingLog = existingLogs.stream()
                        .filter(log -> log.getScheduledDateTime().equals(scheduledTime))
                        .findFirst()
                        .orElse(null);
                    if (existingLog != null) {
                        upcomingLogs.add(existingLog);
                    }
                }
            }
            
            // Add any existing logs that might be in the future but beyond 24 hours
            existingLogs.stream()
                .filter(log -> log.getScheduledDateTime().isAfter(endDate))
                .forEach(upcomingLogs::add);
                
        } else {
            // For non-hourly habits, get logs for the next 7 days
            LocalDateTime endDate = now.plusDays(7);
            upcomingLogs = logRepo.findByHabitAndScheduledDateTimeBetweenOrderByScheduledDateTimeAsc(
                habit, now, endDate);
                
            // If no logs found, generate the next occurrence
            if (upcomingLogs.isEmpty()) {
                HabitLog nextOccurrence = new HabitLog();
                nextOccurrence.setHabit(habit);
                nextOccurrence.setScheduledDateTime(calculateNextScheduledTime(habit, now.atZone(ZoneId.systemDefault())));
                nextOccurrence.setCompleted(false);
                upcomingLogs.add(nextOccurrence);
            }
        }
        
        return upcomingLogs;
    }
    
    /**
     * Get today's active habits based on their recurrence patterns
     */
    @Transactional
    public List<Habit> getTodaysHabits() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<Habit> habits = habitRepo.findByArchivedFalse();
        List<Habit> result = new ArrayList<>();

        for (Habit habit : habits) {
            if (habit.getRecurrence() == null) continue;

            // For hourly habits, we need to generate logs for the entire day
            if (habit.getRecurrence() == Recurrence.HOURLY) {
                // Generate logs for each hour of the day
                List<HabitLog> todaysLogs = new ArrayList<>();
                LocalDateTime currentHour = startOfDay;
                
                // Get all existing logs for today
                List<HabitLog> existingLogs = logRepo.findByHabitAndScheduledDateTimeBetween(
                    habit, startOfDay, endOfDay);
                
                // Generate logs for each hour of the day
                while (currentHour.isBefore(endOfDay)) {
                    LocalDateTime hourStart = currentHour;
                    LocalDateTime hourEnd = currentHour.plusHours(1);
                    
                    // Find existing log for this hour
                    Optional<HabitLog> existingLog = existingLogs.stream()
                        .filter(log -> !log.getScheduledDateTime().isBefore(hourStart) && 
                                     log.getScheduledDateTime().isBefore(hourEnd))
                        .findFirst();
                    
                    if (existingLog.isPresent()) {
                        todaysLogs.add(existingLog.get());
                    } else {
                        // Create a new log for this hour if none exists
                        HabitLog log = new HabitLog();
                        log.setHabit(habit);
                        log.setScheduledDateTime(hourStart);
                        log.setCompleted(false);
                        
                        // Mark as missed if the hour has passed and not completed
                        if (hourEnd.isBefore(now)) {
                            log.setMissed(true);
                            log.setMissedDateTime(hourEnd);
                        }
                        
                        // Save the new log to the database
                        log = logRepo.save(log);
                        todaysLogs.add(log);
                    }
                    
                    currentHour = hourEnd;
                }
                
                // Clear existing logs and add all new ones to avoid orphan removal issues
                habit.getLogs().clear();
                habit.getLogs().addAll(todaysLogs);
                
                // Save the habit to update the logs collection
                habit = habitRepo.save(habit);
                
                // Add to result if there are any logs for today
                if (!todaysLogs.isEmpty()) {
                    result.add(habit);
                }
                
                continue;
            }
            
            // For non-hourly habits, use the existing logic
            switch (habit.getRecurrence()) {
                case DAILY:
                    // For daily habits, check if there's an uncompleted log for today
                    List<HabitLog> todayLogs = logRepo.findByHabitAndScheduledDateTimeBetween(
                        habit, startOfDay, endOfDay);
                    
                    boolean dailyCompleted = todayLogs.stream()
                        .anyMatch(log -> log.getCompleted() != null && log.getCompleted());
                    
                    if (!dailyCompleted) {
                        result.add(habit);
                    }
                    break;
                    
                case WEEKLY:
                    if (habit.getWeeklyDay() != null && today.getDayOfWeek() == habit.getWeeklyDay()) {
                        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
                        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
                        LocalDateTime endOfWeekDateTime = startOfWeekDateTime.plusWeeks(1);
                        
                        boolean weeklyCompleted = logRepo.findByHabitAndScheduledDateTimeBetween(
                            habit, startOfWeekDateTime, endOfWeekDateTime)
                            .stream()
                            .anyMatch(log -> log.getCompleted() != null && log.getCompleted());
                        
                        if (!weeklyCompleted) {
                            result.add(habit);
                        }
                    }
                    break;
                    
                case MONTHLY:
                    if (habit.getMonthlyDay() != null && today.getDayOfMonth() == habit.getMonthlyDay()) {
                        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
                        LocalDateTime startOfNextMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
                        
                        boolean monthlyCompleted = logRepo.findByHabitAndScheduledDateTimeBetween(
                            habit, startOfMonth, startOfNextMonth)
                            .stream()
                            .anyMatch(log -> log.getCompleted() != null && log.getCompleted());
                        
                        if (!monthlyCompleted) {
                            result.add(habit);
                        }
                    }
                    break;
                    
                case YEARLY:
                    if (habit.getYearlyMonth() != null && habit.getYearlyDay() != null &&
                        today.getMonthValue() == habit.getYearlyMonth() && 
                        today.getDayOfMonth() == habit.getYearlyDay()) {
                        
                        LocalDateTime startOfYear = today.withDayOfYear(1).atStartOfDay();
                        LocalDateTime startOfNextYear = today.plusYears(1).withDayOfYear(1).atStartOfDay();
                        
                        boolean yearlyCompleted = logRepo.findByHabitAndScheduledDateTimeBetween(
                            habit, startOfYear, startOfNextYear)
                            .stream()
                            .anyMatch(log -> log.getCompleted() != null && log.getCompleted());
                        
                        if (!yearlyCompleted) {
                            result.add(habit);
                        }
                    }
                    break;
            }
        }
        
        return result;
    }

    public List<HabitLog> getLogsBetween(Habit habit, LocalDateTime start, LocalDateTime end) {
        return logRepo.findByHabitAndScheduledDateTimeBetween(habit, start, end);
    }

    public List<HabitLog> getCompletedLogs(Habit habit) {
        return logRepo.findByHabitAndCompletedTrue(habit);
    }

    @Transactional
    public HabitLog markDone(Long logId) {
        HabitLog log = logRepo.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found with id: " + logId));

        log.setCompleted(true);
        log.setCompletedDateTime(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());

        // Update habit streaks
        updateHabitStreaks(log.getHabit());

        return logRepo.save(log);
    }

    @Transactional
    public HabitLog skipHabit(Long logId) {
        HabitLog log = logRepo.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found with id: " + logId));

        log.setSkipped(true);
        log.setUpdatedAt(LocalDateTime.now());

        return logRepo.save(log);
    }

    @Transactional
    public void deleteHabit(Long id) {
        if (id == null) return;

        // First delete all logs for this habit
        logRepo.deleteByHabitId(id);

        // Then delete the habit
        habitRepo.deleteById(id);
    }

    public void updateHabitStreaks(Habit habit) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (habit.getLastCompleted() != null && habit.getLastCompleted().equals(today)) {
            return;
        }

        if (habit.getLastCompleted() != null && habit.getLastCompleted().equals(yesterday)) {
            habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        } else if (habit.getLastCompleted() == null || habit.getLastCompleted().isBefore(yesterday)) {
            habit.setCurrentStreak(1);
        }

        habit.setLastCompleted(today);
        habitRepo.save(habit);
    }
    
    /**
     * Get a page of habit logs for a specific habit
     * @param habit The habit to get logs for
     * @param page The page number (0-based)
     * @param size The number of items per page
     * @return A page of habit logs
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<HabitLog> getLogsPage(Habit habit, int page, int size) {
        if (habit == null) {
            throw new IllegalArgumentException("Habit cannot be null");
        }
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by("scheduledDateTime").descending());
                
        return logRepo.findByHabit(habit, pageable);
    }
}