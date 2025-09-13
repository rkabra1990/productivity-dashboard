package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service responsible for scheduling habits and generating logs
 */
@Service
public class HabitSchedulerService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SleepWindow sleepWindow;
    private final TelegramService telegramService;
    private final HabitService habitService;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    
    // Default grace period in minutes
    private static final int DEFAULT_GRACE_PERIOD = 15;
    
    /**
     * Checks if the current time is within the grace period after the scheduled time
     * @param scheduledTime The scheduled time of the habit
     * @param currentTime The current time
     * @param gracePeriodMinutes The grace period in minutes
     * @return true if within grace period, false otherwise
     */
    private boolean isWithinGracePeriod(LocalDateTime scheduledTime, LocalDateTime currentTime, int gracePeriodMinutes) {
        if (gracePeriodMinutes <= 0) {
            return false;
        }
        Duration duration = Duration.between(scheduledTime, currentTime);
        return !duration.isNegative() && duration.toMinutes() <= gracePeriodMinutes;
    }
    
    @Autowired
    public HabitSchedulerService(HabitRepository habitRepository, 
                               HabitLogRepository habitLogRepository,
                               SleepWindow sleepWindow,
                               TelegramService telegramService,
                               HabitService habitService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.sleepWindow = sleepWindow;
        this.telegramService = telegramService;
        this.habitService = habitService;
    }

    /**
     * Scheduled task that runs every minute to check for habits that need scheduling
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void scheduleHabits() {
        try {
            List<Habit> activeHabits = habitRepository.findByArchivedFalse();
            ZonedDateTime now = ZonedDateTime.now();
            
            for (Habit habit : activeHabits) {
                try {
                    processHabit(habit, now);
                } catch (Exception e) {
                    System.err.println("Error processing habit " + habit.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    
                    // Notify admin of the error
                    try {
                        String errorMessage = String.format("❌ *Error Processing Habit*\n\n" +
                            "*%s* (ID: %d)\n" +
                            "Error: %s",
                            habit.getName(),
                            habit.getId(),
                            e.getMessage()
                        );
                        telegramService.sendMessage(errorMessage);
                    } catch (Exception ex) {
                        System.err.println("Failed to send error notification: " + ex.getMessage());
                    }
                    
                    // Process any missed habits that need to be marked as such
                    habitService.checkAndMarkMissedHabits(habit, now);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in scheduleHabits: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Process a single habit for scheduling
     */
    private void processHabit(Habit habit, ZonedDateTime now) {
        try {
            String timeZone = habit.getTimeZone() != null ? habit.getTimeZone() : "UTC";
            ZoneId zoneId = ZoneId.of(timeZone);
            ZonedDateTime zonedNow = now.withZoneSameInstant(zoneId);
            
            // Check if we need to generate logs for multiple daily occurrences
            if (habit.getRecurrence() == Recurrence.DAILY && 
                habit.isAllowMultipleDaily() &&
                habit.getDailyReminderTimes() != null && 
                !habit.getDailyReminderTimes().isEmpty()) {
                
                generateMultipleDailyLogs(habit, zonedNow);
            } else {
                // Handle single occurrence habits
                LocalDateTime nextScheduledTime = calculateNextScheduledTime(habit, zonedNow);
                if (nextScheduledTime != null) {
                    habitService.createHabitLogIfNotExists(habit, nextScheduledTime, zoneId);
                }
            }
            
            // Check for and mark any missed habits
            habitService.checkAndMarkMissedHabits(habit, zonedNow);
            
            // Send notifications for upcoming habits
            sendUpcomingNotifications(habit, zonedNow);
            
        } catch (Exception e) {
            System.err.println("Error in processHabit for habit " + habit.getId() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Sends notifications for upcoming habits
     * @param habit The habit to check for upcoming notifications
     * @param now Current time in the habit's timezone
     */
    private void sendUpcomingNotifications(Habit habit, ZonedDateTime now) {
        try {
            // Only send notifications for active (non-archived) habits
            if (habit.getArchived()) {
                return;
            }
            
            // Get the next scheduled time for the habit
            LocalDateTime nextScheduledTime = calculateNextScheduledTime(habit, now);
            if (nextScheduledTime == null) {
                return;
            }
            
            // Convert to ZonedDateTime for comparison
            ZonedDateTime nextScheduledZoned = nextScheduledTime.atZone(now.getZone());
            
            // Check if the next scheduled time is within the next 15 minutes
            Duration timeUntilNext = Duration.between(now, nextScheduledZoned);
            if (!timeUntilNext.isNegative() && timeUntilNext.toMinutes() <= 15) {
                // Format the notification message
                String message = String.format("🔔 Reminder: %s is scheduled for %s",
                    habit.getName(),
                    nextScheduledTime.format(DateTimeFormatter.ofPattern("h:mm a")));
                
                // Send notification (assuming telegramService is available)
                if (telegramService != null) {
                    telegramService.sendMessage(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending upcoming notification for habit " + habit.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate logs for multiple daily occurrences of a habit
     */
    private void generateMultipleDailyLogs(Habit habit, ZonedDateTime zonedNow) {
        try {
            String timeZone = habit.getTimeZone() != null ? habit.getTimeZone() : "UTC";
            ZoneId zoneId = ZoneId.of(timeZone);
            LocalDate today = zonedNow.toLocalDate();
            
            if (habit.getDailyReminderTimes() == null || habit.getDailyReminderTimes().isEmpty()) {
                return;
            }
            
            for (LocalTime reminderTime : habit.getDailyReminderTimes()) {
                if (reminderTime == null) continue;
                
                LocalDateTime scheduledTime = LocalDateTime.of(today, reminderTime);
                
                // Skip if this time has already passed today and we're not in grace period
                if (scheduledTime.isBefore(zonedNow.toLocalDateTime())) {
                    // Check if this was missed
                    if (!habitService.isLogExistsForTime(habit, scheduledTime) && 
                        !isWithinGracePeriod(scheduledTime, zonedNow.toLocalDateTime(), 
                            habit.getGracePeriodMinutes() != null ? habit.getGracePeriodMinutes() : DEFAULT_GRACE_PERIOD)) {
                        habitService.markHabitAsMissed(habit, scheduledTime);
                    }
                    continue;
                }
                
                // Create log for future or current (within grace period) times
                habitService.createHabitLogIfNotExists(habit, scheduledTime, zoneId);
            }
        } catch (Exception e) {
            System.err.println("Error generating daily logs for habit " + habit.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate the next scheduled time for a habit based on its recurrence pattern
     */
    private LocalDateTime calculateNextScheduledTime(Habit habit, ZonedDateTime now) {
        if (habit == null || habit.getRecurrence() == null) {
            return null;
        }

        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalTime scheduledTime = habit.getScheduledTime() != null ? 
            habit.getScheduledTime() : LocalTime.NOON;
            
        switch (habit.getRecurrence()) {
            case DAILY:
                LocalDateTime nextDaily = LocalDateTime.of(nowLocal.toLocalDate(), scheduledTime);
                return nextDaily.isAfter(nowLocal) ? nextDaily : nextDaily.plusDays(1);
                
            case WEEKLY:
                if (habit.getWeeklyDay() == null) {
                    return null;
                }
                LocalDateTime nextWeekly = LocalDateTime.of(nowLocal.toLocalDate(), scheduledTime);
                int daysUntilNext = (habit.getWeeklyDay().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
                daysUntilNext = daysUntilNext == 0 ? 7 : daysUntilNext; // If same day but time has passed, schedule for next week
                return nextWeekly.plusDays(daysUntilNext);
                
            case MONTHLY:
                if (habit.getMonthlyDay() == null) {
                    return null;
                }
                int dayOfMonth = Math.min(habit.getMonthlyDay(), nowLocal.toLocalDate().lengthOfMonth());
                LocalDateTime nextMonthly = LocalDateTime.of(
                    nowLocal.getYear(), 
                    nowLocal.getMonth(), 
                    dayOfMonth, 
                    scheduledTime.getHour(), 
                    scheduledTime.getMinute()
                );
                
                if (nextMonthly.isAfter(nowLocal)) {
                    return nextMonthly;
                }
                return nextMonthly.plusMonths(1).withDayOfMonth(
                    Math.min(habit.getMonthlyDay(), nextMonthly.plusMonths(1).toLocalDate().lengthOfMonth())
                );
                
            case YEARLY:
                if (habit.getYearlyMonth() == null || habit.getYearlyDay() == null) {
                    return null;
                }
                YearMonth yearMonth = YearMonth.of(nowLocal.getYear(), habit.getYearlyMonth());
                int day = Math.min(habit.getYearlyDay(), yearMonth.lengthOfMonth());
                LocalDateTime nextYearly = LocalDateTime.of(
                    nowLocal.getYear(), 
                    habit.getYearlyMonth(), 
                    day, 
                    scheduledTime.getHour(), 
                    scheduledTime.getMinute()
                );
                
                if (nextYearly.isAfter(nowLocal)) {
                    return nextYearly;
                }
                return nextYearly.plusYears(1);
                
            default:
            // Find all uncompleted logs that are past their scheduled time + grace period
            List<HabitLog> missedLogs = habitLogRepository.findByHabitAndCompletedFalseAndMissedFalseAndScheduledDateTimeBefore(
                habit, 
                now.minusMinutes(DEFAULT_GRACE_PERIOD).toLocalDateTime()
            );
            
            // Mark each missed habit
            for (HabitLog log : missedLogs) {
                // Skip if this is a future occurrence
                if (log.getScheduledDateTime().isAfter(now.toLocalDateTime())) {
                    continue;
                }
                
                // Mark as missed
                habitService.markHabitAsMissed(habit, log.getScheduledDateTime());
            }
            
            // Update last check time
            habit.setLastScheduled(now.toLocalDateTime());
            habit.setUpdatedAt(LocalDateTime.now());
            habitRepository.save(habit);
            
            // Send notification for the next scheduled habit
            try {
                if (scheduledTime != null) {
                    String message = String.format(
                        "✅ *New Habit Scheduled*\n\n" +
                        "*%s*\n" +
                        "⏰ %s\n\n" +
                        "_You'll be notified when it's time to complete this habit._",
                        habit.getName(),
                        formatDateTime(scheduledTime)
                    );
                    
                    telegramService.sendMessage(message);
                }
            } catch (Exception ex) {
                System.err.println("Failed to send notification: " + ex.getMessage());
                ex.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Error checking missed habits for habit " + habit.getId() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Try to notify admin about the error
            try {
                String errorMsg = String.format("❌ *Error checking missed habits*\n\n" +
                    "*%s* (ID: %d)\n" +
                    "Error: %s",
                    habit.getName(),
                    habit.getId(),
                    e.getMessage());
                
                // Log the error
                HabitLog errorLog = new HabitLog();
                errorLog.setHabit(habit);
                errorLog.setScheduledDateTime(LocalDateTime.now());
                errorLog.setError(true);
                errorLog.setErrorMessage(e.getMessage());
                habitLogRepository.save(errorLog);
                
                // Update habit's next scheduled time if needed
                if (scheduledTime != null) {
                    habit.setNextScheduled(scheduledTime);
                    habitRepository.save(habit);
                }
            } catch (Exception ex) {
                System.err.println("Failed to log error: " + ex.getMessage());
                ex.printStackTrace();
            }
                    System.err.println("Failed to send habit scheduled notification: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Process any missed habits that need to be marked as such
     */
    @Transactional
    public void processMissedHabits() {
        List<HabitLog> unprocessedMissedLogs = habitLogRepository.findUnprocessedMissedLogs();
        LocalDateTime now = LocalDateTime.now();
        
        for (HabitLog log : unprocessedMissedLogs) {
            try {
                // Only process if the grace period has passed
                if (log.getScheduledDateTime().plusMinutes(log.getHabit().getGracePeriodMinutes())
                        .isBefore(now)) {
                    log.setMissed(true);
                    log.setMissedDateTime(now);
                    habitLogRepository.save(log);
                    
                    // Update the habit's missed count
                    Habit habit = log.getHabit();
                    habit.setMissedCount(habit.getMissedCount() + 1);
                    habitRepository.save(habit);
                    
                    // Send missed notification
                    try {
                        String message = String.format(
                            "❌ *Missed Habit*\n\n" +
                            "*%s*\n" +
                            "⏰ %s\n\n" +
                            "_You can still complete this habit late if you want!_\n" +
                            "Type /complete to mark as done.",
                            habit.getName(),
                            formatDateTime(log.getScheduledDateTime())
                        );
                        
                        telegramService.sendMessage(message);
                    } catch (Exception e) {
                        System.err.println("Failed to send missed habit notification: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing missed habit log " + log.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
                "_You can still complete this habit late if you want!_\n" +
                "Type /complete to mark as done.",
                habit.getName(),
                formatDateTime(scheduledTime)
            );
            
            telegramService.sendMessage(message);
        } catch (Exception e) {
            System.err.println("Failed to send missed habit notification: " + e.getMessage());
        }            
    }
    
    /**
     * Send notifications for upcoming habits
     */
    private void sendUpcomingNotifications(Habit habit, ZonedDateTime zonedNow) {
        try {
            LocalDateTime now = zonedNow.toLocalDateTime();
            LocalDateTime notificationTime = now.plusMinutes(5);
            
            // Find logs that are due soon (within the next 15 minutes)
            List<HabitLog> upcomingLogs = habitLogRepository.findByHabitAndScheduledDateTimeBetweenAndNotifSentFalse(
                habit, 
                now.plusMinutes(1), // Start from 1 minute from now to avoid duplicate notifications
                now.plusMinutes(15) // Look ahead 15 minutes
            );
            
            for (HabitLog log : upcomingLogs) {
                try {
                    String message = String.format(
                        "🔔 *Upcoming Habit*\n\n" +
                        "*%s*\n" +
                        "⏰ %s\n\n" +
                        "_Time to complete your habit!_",
                        habit.getName(),
                        formatDateTime(log.getScheduledDateTime())
                    );
                    
                    // Send notification via Telegram
                    telegramService.sendMessage(message);
                    
                    // Mark as notified
                    log.setNotifSent(true);
                    habitLogRepository.save(log);
                    
                    System.out.println("Sent notification for habit: " + habit.getName() + " at " + now);
                    
                } catch (Exception e) {
                    System.err.println("Failed to send Telegram notification for habit " + 
                                     habit.getId() + 
                                     ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in sendUpcomingNotifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ... (other helper methods remain the same)
}
