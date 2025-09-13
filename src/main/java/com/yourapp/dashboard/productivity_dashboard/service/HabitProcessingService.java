package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HabitProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(HabitProcessingService.class);

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    @Autowired
    public HabitProcessingService(HabitRepository habitRepository, HabitLogRepository habitLogRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public void processHabitsForToday() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        try {
            List<Habit> habits = habitRepository.findByArchivedFalse();
            logger.info("Processing {} habits for today", habits.size());

            for (Habit habit : habits) {
                try {
                    if (habit.getRecurrence() == Recurrence.HOURLY) {
                        processHourlyHabit(habit, now, startOfDay, endOfDay);
                    } else {
                        processSingleHabit(habit, now, startOfDay, endOfDay);
                    }
                } catch (Exception e) {
                    logger.error("Error processing habit: " + habit.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in processHabitsForToday", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<HabitLog> processHourlyHabit(Habit habit, LocalDateTime now, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        List<HabitLog> processedLogs = new ArrayList<>();
        try {
            // Get existing logs for today
            List<HabitLog> existingLogs = habitLogRepository.findByHabitAndScheduledDateTimeBetween(
                habit, startOfDay, endOfDay);
            
            LocalDateTime currentHour = startOfDay;
            
            // Generate logs for each hour in the day
            while (currentHour.isBefore(endOfDay)) {
                final LocalDateTime scheduledTime = currentHour; // effectively final for lambda
                
                // Find or create log for this hour
                Optional<HabitLog> existingLog = existingLogs.stream()
                    .filter(log -> log.getScheduledDateTime().equals(scheduledTime))
                    .findFirst();

                HabitLog log;
                if (existingLog.isPresent()) {
                    log = existingLog.get();
                } else {
                    log = new HabitLog();
                    log.setHabit(habit);
                    log.setScheduledDateTime(scheduledTime);
                    log.setCompleted(false);
                    log = habitLogRepository.save(log);
                }
                
                // Add to processed logs
                processedLogs.add(log);
                
                // Move to next hour
                currentHour = currentHour.plusHours(1);
            }

        } catch (Exception e) {
            logger.error("Error processing hourly habit: " + (habit != null ? habit.getId() : "unknown"), e);
            return processedLogs; // Return whatever logs were processed before the error
        }
        
        return processedLogs; // Return all processed logs
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleHabit(Habit habit, LocalDateTime now, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        // Get the latest log for today
        Optional<HabitLog> existingLog = habitLogRepository
            .findTopByHabitAndScheduledDateTimeBetweenOrderByScheduledDateTimeDesc(
                habit, startOfDay, endOfDay);

        if (existingLog.isEmpty()) {
            // Create a new log if none exists for today
            HabitLog newLog = new HabitLog();
            newLog.setHabit(habit);
            newLog.setScheduledDateTime(now);
            newLog.setCompleted(false);
            habitLogRepository.save(newLog);
            logger.debug("Created new log for habit: {}", habit.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Habit> getTodaysHabits() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Habit> habits = habitRepository.findByArchivedFalse();
        List<Habit> result = new ArrayList<>();

        for (Habit habit : habits) {
            try {
                List<HabitLog> todaysLogs = habitLogRepository
                    .findByHabitAndScheduledDateTimeBetween(habit, startOfDay, endOfDay);
                
                if (!todaysLogs.isEmpty()) {
                    habit.setLogs(todaysLogs);
                    result.add(habit);
                }
            } catch (Exception e) {
                logger.error("Error fetching logs for habit: " + habit.getId(), e);
            }
        }

        return result;
    }
}
