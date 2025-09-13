package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
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
                    if (habit.getRecurrence() == Habit.Recurrence.HOURLY) {
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
    public void processHourlyHabit(Habit habit, LocalDateTime now, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        try {
            // Get existing logs for today
            List<HabitLog> existingLogs = habitLogRepository.findByHabitAndScheduledDateTimeBetween(
                habit, startOfDay, endOfDay);
            
            List<HabitLog> logsToSave = new ArrayList<>();
            LocalDateTime currentHour = startOfDay;
            int totalHours = 0;
            int completedHours = 0;

            // Process each hour of the day
            while (currentHour.isBefore(endOfDay)) {
                totalHours++;
                LocalDateTime hourStart = currentHour;
                LocalDateTime hourEnd = currentHour.plusHours(1);

                // Find existing log for this hour
                Optional<HabitLog> existingLog = existingLogs.stream()
                    .filter(log -> !log.getScheduledDateTime().isBefore(hourStart) &&
                                 log.getScheduledDateTime().isBefore(hourEnd))
                    .findFirst();

                HabitLog log;
                if (existingLog.isPresent()) {
                    log = existingLog.get();
                    if (log.getCompleted()) {
                        completedHours++;
                    } else if (hourEnd.isBefore(now)) {
                        // Mark as missed if the hour has passed and not completed
                        log.setMissed(true);
                        log.setMissedDateTime(hourEnd);
                        logsToSave.add(log);
                    }
                } else if (hourEnd.isBefore(now)) {
                    // Create and mark as missed if the hour has passed
                    log = new HabitLog();
                    log.setHabit(habit);
                    log.setScheduledDateTime(hourStart);
                    log.setMissed(true);
                    log.setMissedDateTime(hourEnd);
                    logsToSave.add(log);
                } else if (currentHour.isBefore(now)) {
                    // For current hour, just create the log
                    log = new HabitLog();
                    log.setHabit(habit);
                    log.setScheduledDateTime(hourStart);
                    logsToSave.add(log);
                }
                
                currentHour = hourEnd;
            }

            // Save all logs in a single transaction
            if (!logsToSave.isEmpty()) {
                habitLogRepository.saveAll(logsToSave);
            }

        } catch (Exception e) {
            logger.error("Error processing hourly habit: " + habit.getId(), e);
            throw e;
        }
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
