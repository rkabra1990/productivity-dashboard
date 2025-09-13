package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HabitLogService {
    private static final Logger logger = LoggerFactory.getLogger(HabitLogService.class);
    
    private final HabitLogRepository logRepo;
    
    @Autowired
    public HabitLogService(HabitLogRepository logRepo) {
        this.logRepo = logRepo;
    }
    
    @Transactional
    public HabitLog saveLog(HabitLog log) {
        return logRepo.save(log);
    }
    
    @Transactional
    public List<HabitLog> saveAllLogs(List<HabitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logRepo.saveAll(logs);
    }
    
    @Transactional
    public HabitLog markLogAsMissed(HabitLog log, LocalDateTime missedTime) {
        log.setMissed(true);
        log.setMissedDateTime(missedTime);
        return logRepo.save(log);
    }
    
    @Transactional(readOnly = true)
    public List<HabitLog> findByHabitAndScheduledDateTimeBetween(Habit habit, LocalDateTime start, LocalDateTime end) {
        return logRepo.findByHabitAndScheduledDateTimeBetween(habit, start, end);
    }
    
    @Transactional(readOnly = true)
    public Optional<HabitLog> findFirstByHabitAndScheduledDateTimeBetween(Habit habit, LocalDateTime start, LocalDateTime end) {
        return logRepo.findFirstByHabitAndScheduledDateTimeBetween(habit, start, end);
    }
}
