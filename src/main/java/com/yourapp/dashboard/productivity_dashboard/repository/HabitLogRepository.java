package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    // Basic CRUD operations
    @Override
    Optional<HabitLog> findById(Long id);
    
    // Find logs by habit and date range
    List<HabitLog> findByHabitAndScheduledDateTimeBetween(
        Habit habit, LocalDateTime start, LocalDateTime end);
    
    // Find logs by habit
    List<HabitLog> findByHabit(Habit habit);
    
    // Find logs by habit and completion status
    List<HabitLog> findByHabitAndCompleted(Habit habit, Boolean completed);
    
    // Find logs by completion status
    List<HabitLog> findByCompleted(Boolean completed);
    
    // Find today's logs for a habit
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "FUNCTION('DATE', l.scheduledDateTime) = CURRENT_DATE")
    List<HabitLog> findTodaysLogsForHabit(@Param("habit") Habit habit);
    
    // Find logs scheduled between two dates
    List<HabitLog> findByScheduledDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    // Check if a log exists for a specific habit and time
    boolean existsByHabitAndScheduledDateTime(Habit habit, LocalDateTime scheduledDateTime);
    
    // Find logs by habit and completion status within a date range
    List<HabitLog> findByHabitAndCompletedAndScheduledDateTimeBetween(
        Habit habit, Boolean completed, LocalDateTime start, LocalDateTime end);
    
    // Count completed logs for a habit within a date range
    long countByHabitAndCompletedAndScheduledDateTimeBetween(
        Habit habit, Boolean completed, LocalDateTime start, LocalDateTime end);
    
    // Get the most recent log for a habit
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit " +
           "ORDER BY l.scheduledDateTime DESC LIMIT 1")
    Optional<HabitLog> findMostRecentByHabit(@Param("habit") Habit habit);
    
    // Get the first log for a habit after a specific date
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit " +
           "AND l.scheduledDateTime > :afterDate ORDER BY l.scheduledDateTime ASC")
    List<HabitLog> findNextLogsAfterDate(
        @Param("habit") Habit habit, 
        @Param("afterDate") LocalDateTime afterDate);
    
    // Delete old logs
    @Modifying
    @Query("DELETE FROM HabitLog l WHERE l.scheduledDateTime < :cutoff")
    void deleteByScheduledDateTimeBefore(@Param("cutoff") LocalDateTime cutoff);
    
    // Find logs for a specific date
    @Query("SELECT l FROM HabitLog l WHERE FUNCTION('DATE', l.scheduledDateTime) = :date")
    List<HabitLog> findByDate(@Param("date") LocalDate date);
    
    // Find completed logs for a habit
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND l.completed = true")
    List<HabitLog> findByHabitAndCompletedTrue(@Param("habit") Habit habit);
    
    // Find logs by habit and scheduled date (time agnostic)
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "FUNCTION('DATE', l.scheduledDateTime) = FUNCTION('DATE', :date)")
    List<HabitLog> findByHabitAndDate(
        @Param("habit") Habit habit, 
        @Param("date") LocalDateTime date);
    
    // Find upcoming logs for a habit
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "l.scheduledDateTime > :now ORDER BY l.scheduledDateTime ASC")
    List<HabitLog> findUpcomingLogs(
        @Param("habit") Habit habit, 
        @Param("now") LocalDateTime now);

    org.springframework.data.domain.Page<HabitLog> findByHabitOrderByScheduledDateTimeDesc(Habit habit, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("DELETE FROM HabitLog h WHERE h.habit.id = :habitId")
    void deleteByHabitId(@Param("habitId") Long habitId);

    List<HabitLog> findByCompletedFalseAndNotifSentFalseAndScheduledDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<HabitLog> findByHabitAndScheduledDateTimeAfterOrderByScheduledDateTimeAsc(Habit habit, LocalDateTime now);
}
