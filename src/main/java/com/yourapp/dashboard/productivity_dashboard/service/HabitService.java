package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.config.SleepWindow;
import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HabitService {

    @Autowired private HabitRepository habitRepo;
    @Autowired private HabitLogRepository logRepo;
    @Autowired private SleepWindow sleepWindow;

    public List<Habit> getAllHabits() {
        return habitRepo.findByArchivedFalse();
    }
    
    public List<Habit> getArchivedHabits() {
        return habitRepo.findByArchivedTrue();
    }
    
    public Optional<Habit> getHabitById(Long id) {
        return habitRepo.findById(id);
    }

    @Transactional
    public Habit createHabit(Habit habit) {
        habit.setArchived(false);
        habit.setCurrentStreak(0);
        habit.setBestStreak(0);
        habit.setLastCompleted(null);
        return habitRepo.save(habit);
    }
    
    @Transactional
    public void completeHabit(Long habitId) {
        Habit habit = habitRepo.findById(habitId)
            .orElseThrow(() -> new RuntimeException("Habit not found"));
            
        // Create a new log entry for this completion
        HabitLog log = new HabitLog();
        log.setHabit(habit);
        log.setScheduledDateTime(LocalDateTime.now());
        log.setCompleted(true);
        logRepo.save(log);
        
        // Update streak
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // If the last completion was yesterday, increment the streak
        // Otherwise, reset to 1
        if (habit.getLastCompleted() != null && 
            habit.getLastCompleted().equals(yesterday)) {
            habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        } else if (habit.getLastCompleted() == null || 
                  !habit.getLastCompleted().equals(today)) {
            habit.setCurrentStreak(1);
        }
        
        // Update best streak if current is better
        if (habit.getCurrentStreak() > habit.getBestStreak()) {
            habit.setBestStreak(habit.getCurrentStreak());
        }
        
        habit.setLastCompleted(today);
        habitRepo.save(habit);
    }
    
    @Transactional
    public void archiveHabit(Long habitId) {
        habitRepo.findById(habitId).ifPresent(habit -> {
            habit.setArchived(true);
            habitRepo.save(habit);
        });
    }
    
    @Transactional
    public void unarchiveHabit(Long habitId) {
        habitRepo.findById(habitId).ifPresent(habit -> {
            habit.setArchived(false);
            habitRepo.save(habit);
        });
    }

    public void generateTodayLogsForHabit(Habit habit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = null;
        LocalTime currentTime = now.toLocalTime();

        // Skip if current time is within sleep window
        if (SleepWindow.isAsleep(currentTime, sleepWindow.getSleepStart(), sleepWindow.getSleepEnd())) {
            return;
        }

        switch (habit.getRecurrence()) {
            case HOURLY:
                // For hourly, find next slot after current time that's outside sleep window
                scheduledTime = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                        .plusHours(1)
                        .withMinute(habit.getScheduledTime().getMinute())
                        .withSecond(0).withNano(0);
                
                // If next scheduled time is within sleep window, move to after sleep end
                while (SleepWindow.isAsleep(scheduledTime.toLocalTime(), 
                                         sleepWindow.getSleepStart(), 
                                         sleepWindow.getSleepEnd())) {
                    scheduledTime = scheduledTime.plusHours(1);
                }
                break;
                
            case DAILY:
                scheduledTime = now.toLocalDate().atTime(habit.getScheduledTime());
                if (scheduledTime.isBefore(now)) {
                    scheduledTime = scheduledTime.plusDays(1);
                }
                break;
                
            case WEEKLY:
                DayOfWeek targetDay = habit.getWeeklyDay() != null ? habit.getWeeklyDay() : now.getDayOfWeek();
                scheduledTime = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(targetDay))
                        .with(habit.getScheduledTime());
                if (scheduledTime.isBefore(now)) {
                    scheduledTime = scheduledTime.plusWeeks(1);
                }
                break;
                
            case MONTHLY:
                int dayOfMonth = habit.getMonthlyDay() != null ? habit.getMonthlyDay() : 1;
                LocalDate tentative = now.toLocalDate().withDayOfMonth(Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth()));
                scheduledTime = tentative.atTime(habit.getScheduledTime());
                if (scheduledTime.isBefore(now)) {
                    LocalDate nextMonth = now.toLocalDate().plusMonths(1);
                    tentative = nextMonth.withDayOfMonth(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
                    scheduledTime = tentative.atTime(habit.getScheduledTime());
                }
                break;
                
            case YEARLY:
                int month = habit.getYearlyMonth() != null ? habit.getYearlyMonth() : 1;
                int day = habit.getYearlyDay() != null ? habit.getYearlyDay() : 1;
                YearMonth ym = YearMonth.of(now.getYear(), month);
                day = Math.min(day, ym.lengthOfMonth());
                LocalDate yearlyDate = LocalDate.of(now.getYear(), month, day);
                scheduledTime = yearlyDate.atTime(habit.getScheduledTime());
                if (scheduledTime.isBefore(now)) {
                    ym = YearMonth.of(now.getYear() + 1, month);
                    day = Math.min(day, ym.lengthOfMonth());
                    yearlyDate = LocalDate.of(now.getYear() + 1, month, day);
                    scheduledTime = yearlyDate.atTime(habit.getScheduledTime());
                }
                break;
        }

        if (scheduledTime != null &&
                !logRepo.existsByHabitAndScheduledDateTime(habit, scheduledTime)) {
            HabitLog log = new HabitLog(null, habit, scheduledTime, false);
            logRepo.save(log);
        }
    }

    public List<HabitLog> getUpcomingLogs(Habit habit) {
        return logRepo.findByHabitAndScheduledDateTimeAfterOrderByScheduledDateTimeAsc(
            habit, LocalDateTime.now());
    }
    
    public List<HabitLog> getTodaysHabits() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<Habit> activeHabits = getAllHabits();
        List<HabitLog> todaysLogs = new ArrayList<>();
        
        for (Habit habit : activeHabits) {
            // Eagerly load the logs for this habit
            Habit fullHabit = getHabitById(habit.getId()).orElse(null);
            if (fullHabit == null) continue;
            
            List<HabitLog> logs = logRepo.findByHabitAndScheduledDateTimeBetween(
                fullHabit, startOfDay, endOfDay);
                
            if (!logs.isEmpty()) {
                HabitLog todaysLog = logs.get(0); // Get the first log for today
                // Ensure the log's habit has its logs loaded
                todaysLog.setHabit(fullHabit);
                todaysLogs.add(todaysLog);
            }
        }
        
        // Sort by scheduled time
        return todaysLogs.stream()
            .sorted(Comparator.comparing(HabitLog::getScheduledDateTime))
            .collect(Collectors.toList());
    }

    public List<HabitLog> getLogsBetween(Habit habit, LocalDateTime start, LocalDateTime end) {
        return logRepo.findByHabitAndScheduledDateTimeBetween(habit, start, end);
    }
    
    public List<HabitLog> getCompletedLogs(Habit habit) {
        return logRepo.findByHabitAndCompleted(habit, true);
    }

    @Transactional
    public void markDone(Long logId) {
        logRepo.findById(logId).ifPresent(log -> {
            if (Boolean.FALSE.equals(log.isCompleted())) {
                log.setCompleted(true);
                logRepo.save(log);
                updateHabitStreak(log.getHabit(), true);
            }
        });
    }
    
    @Transactional
    public void skipHabit(Long logId) {
        logRepo.findById(logId).ifPresent(log -> {
            if (Boolean.FALSE.equals(log.isCompleted())) {
                log.setCompleted(false);
                logRepo.save(log);
                updateHabitStreak(log.getHabit(), false);
            }
        });
    }
    
    private void updateHabitStreak(Habit habit, boolean completed) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        if (completed) {
            // If habit was completed today, don't update the streak
            if (today.equals(habit.getLastCompleted())) {
                return;
            }
            
            // If the last completion was yesterday, increment the streak
            if (yesterday.equals(habit.getLastCompleted())) {
                habit.setCurrentStreak(habit.getCurrentStreak() + 1);
            } else if (habit.getLastCompleted() == null || 
                      !habit.getLastCompleted().isAfter(yesterday)) {
                // If no last completion or last completion was before yesterday, reset to 1
                habit.setCurrentStreak(1);
            }
            
            habit.setLastCompleted(today);
            habitRepo.save(habit);
        } else {
            // For skipped habits, we don't break the streak if it was already completed today
            if (today.equals(habit.getLastCompleted())) {
                return;
            }
            
            // If the last completion was before yesterday, we might want to break the streak
            if (habit.getLastCompleted() != null && 
                habit.getLastCompleted().isBefore(yesterday)) {
                habit.setCurrentStreak(0);
                habitRepo.save(habit);
            }
        }
    }
    
    public Map<String, Object> getHabitStats() {
        List<Habit> activeHabits = getAllHabits();
        Map<String, Object> stats = new HashMap<>();
        
        int totalHabits = activeHabits.size();
        int totalCompletedToday = 0;
        int totalScheduledToday = 0;
        int currentStreak = 0;
        
        for (Habit habit : activeHabits) {
            currentStreak = Math.max(currentStreak, habit.getCurrentStreak());
            
            // Get today's logs for this habit
            List<HabitLog> todayLogs = logRepo.findByHabitAndScheduledDateTimeBetween(
                habit, 
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
            );
            
            if (!todayLogs.isEmpty()) {
                totalScheduledToday++;
                if (todayLogs.stream().anyMatch(HabitLog::isCompleted)) {
                    totalCompletedToday++;
                }
            }
        }
        
        double completionRate = totalScheduledToday > 0 ? 
            ((double) totalCompletedToday / totalScheduledToday) * 100.0 : 0.0;
        
        stats.put("totalHabits", totalHabits);
        stats.put("totalCompletedToday", totalCompletedToday);
        stats.put("totalScheduledToday", totalScheduledToday);
        stats.put("completionRate", Double.valueOf(completionRate));
        stats.put("currentStreak", currentStreak);
        
        return stats;
    }

    public List<Habit> getTodayHabits() {
        return habitRepo.findAll();
    }

    public Boolean isCompletedToday(Habit habit) {
        LocalDate today = LocalDate.now();
        List<HabitLog> logs = logRepo.findByHabit(habit);
        return logs.stream()
                .anyMatch(log -> log.getScheduledDateTime().toLocalDate().equals(today) && log.isCompleted());
    }

    public Page<HabitLog> getLogsPage(Habit habit, int page, int size) {
        return logRepo.findByHabitOrderByScheduledDateTimeDesc(habit, PageRequest.of(page, size));
    }

    public Optional<Habit> getHabit(Long id) {
        return habitRepo.findById(id);
    }
    
    @Transactional
    public void deleteHabit(Long id) {
        // First delete all logs for this habit
        logRepo.deleteByHabitId(id);
        // Then delete the habit
        habitRepo.deleteById(id);
    }
}
