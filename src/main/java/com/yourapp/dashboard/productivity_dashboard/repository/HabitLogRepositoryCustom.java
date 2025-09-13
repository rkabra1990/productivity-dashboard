package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;

import java.time.LocalDateTime;
import java.util.List;

public interface HabitLogRepositoryCustom {
    List<HabitLog> findLogsByHabitAndDateRange(Habit habit, LocalDateTime start, LocalDateTime end);
    void markLogsAsProcessed(List<Long> logIds);
}
