package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.config.SleepWindow;
import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    
    private String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(TIME_FORMAT);
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
                            formatDateTime(log.getScheduledDateTime().atZone(ZoneId.systemDefault()))
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
                scheduledTime.format(TIME_FORMAT)
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
                        formatDateTime(log.getScheduledDateTime().atZone(ZoneId.systemDefault()))
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
