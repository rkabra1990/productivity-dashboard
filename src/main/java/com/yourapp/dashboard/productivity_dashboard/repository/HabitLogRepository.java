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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long>, HabitLogRepositoryCustom {
    // Basic CRUD operations
    @Override
    Optional<HabitLog> findById(Long id);
    
    // Find logs by habit and date range with custom implementation for better connection handling
    @Query("SELECT hl FROM HabitLog hl WHERE hl.habit = :habit AND hl.scheduledDateTime BETWEEN :start AND :start")
    @Deprecated
    List<HabitLog> findByHabitAndScheduledDateTimeBetween(
        @Param("habit") Habit habit, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end);
    
    // Find logs by habit
    List<HabitLog> findByHabit(Habit habit);
    
    // Find logs by habit with pagination
    Page<HabitLog> findByHabit(Habit habit, Pageable pageable);
    
    // Find the most recent log for a habit within a date range
    Optional<HabitLog> findTopByHabitAndScheduledDateTimeBetweenOrderByScheduledDateTimeDesc(
        Habit habit, LocalDateTime start, LocalDateTime end);
        
    // Find the first log for a habit within a date range
    Optional<HabitLog> findFirstByHabitAndScheduledDateTimeBetween(
        Habit habit, LocalDateTime start, LocalDateTime end);
    
    // Count completed logs within a date range
    long countByCompletedAndScheduledDateTimeBetween(boolean completed, LocalDateTime start, LocalDateTime end);
    
    // Find logs by habit and completion status
    List<HabitLog> findByHabitAndCompleted(Habit habit, Boolean completed);
    
    // Check if a log exists for a habit within a date range
    boolean existsByHabitAndScheduledDateTimeBetween(Habit habit, LocalDateTime start, LocalDateTime end);
    
    // Find logs by completion status
    List<HabitLog> findByCompleted(Boolean completed);
    
    // Find logs by habit, completion status true, and within a date range
    List<HabitLog> findByHabitAndCompletedTrueAndScheduledDateTimeBetween(
        Habit habit, LocalDateTime start, LocalDateTime end);
    
    // Find most recent logs, ordered by scheduled date/time (newest first)
    List<HabitLog> findTop50ByOrderByScheduledDateTimeDesc();
    
    // Find logs by habit, not completed, not missed, and scheduled before a specific time
    List<HabitLog> findByHabitAndCompletedFalseAndMissedFalseAndScheduledDateTimeBefore(
        Habit habit, 
        LocalDateTime scheduledDateTime
    );
    
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
        Habit habit, boolean completed, LocalDateTime start, LocalDateTime end);
    
    // Count completed logs for a habit within a date range
    long countByHabitAndCompletedAndScheduledDateTimeBetween(
        Habit habit, boolean completed, LocalDateTime start, LocalDateTime end);
        
    // Find logs by habit and scheduled date time
    Optional<HabitLog> findByHabitAndScheduledDateTime(Habit habit, LocalDateTime scheduledDateTime);
    
    // Find logs by habit, within a date range, that haven't been notified yet
    @Query("SELECT hl FROM HabitLog hl WHERE hl.habit = :habit " +
           "AND hl.scheduledDateTime BETWEEN :start AND :end " +
           "AND (hl.notifSent = false OR hl.notifSent IS NULL)")
    List<HabitLog> findByHabitAndScheduledDateTimeBetweenAndNotifSentFalse(
        @Param("habit") Habit habit,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    // Find logs by habit and date range, ordered by scheduled time
    List<HabitLog> findByHabitAndScheduledDateTimeBetweenOrderByScheduledDateTimeAsc(
        Habit habit, LocalDateTime start, LocalDateTime end);
        
    // Find logs by habit and missed status
    List<HabitLog> findByHabitAndMissed(Habit habit, boolean missed);
    
    // Find logs by habit, completion status, and missed status
    List<HabitLog> findByHabitAndCompletedAndMissed(
        Habit habit, boolean completed, boolean missed);
        
    // Find logs by habit and whether they were completed in grace period
    List<HabitLog> findByHabitAndCompletedInGracePeriod(
        Habit habit, boolean completedInGracePeriod);
        
    // Find logs by habit and rescheduled status
    List<HabitLog> findByHabitAndRescheduled(Habit habit, boolean rescheduled);
    
    // Find upcoming logs for a habit after a specific time
    List<HabitLog> findByHabitAndScheduledDateTimeAfterOrderByScheduledDateTimeAsc(
        Habit habit, LocalDateTime after);
    
    // Find logs by habit and date range for a specific timezone
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "l.scheduledDateTime >= :start AND l.scheduledDateTime < :end " +
           "ORDER BY l.scheduledDateTime")
    List<HabitLog> findLogsInTimeRange(
        @Param("habit") Habit habit,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
        
    // Find logs by habit and completion status within a time range, with timezone consideration
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND l.completed = :completed " +
           "AND l.scheduledDateTime >= :start AND l.scheduledDateTime < :end " +
           "ORDER BY l.scheduledDateTime")
    List<HabitLog> findCompletedLogsInTimeRange(
        @Param("habit") Habit habit,
        @Param("completed") boolean completed,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
    
    // Find logs that are due soon (within the next X minutes)
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "l.scheduledDateTime BETWEEN :now AND :dueTime AND " +
           "l.completed = false AND l.missed = false")
    List<HabitLog> findDueSoonLogs(
        @Param("habit") Habit habit,
        @Param("now") LocalDateTime now,
        @Param("dueTime") LocalDateTime dueTime);
    
    // Find logs that were missed but not yet processed
    @Query("SELECT l FROM HabitLog l WHERE l.missed = true AND l.missedDateTime IS NULL")
    List<HabitLog> findUnprocessedMissedLogs();
    
    // Find logs that were completed within grace period
    @Query("SELECT l FROM HabitLog l WHERE l.habit = :habit AND " +
           "l.completedInGracePeriod = true AND " +
           "l.scheduledDateTime >= :start AND l.scheduledDateTime < :end")
    List<HabitLog> findGracePeriodCompletions(
        @Param("habit") Habit habit,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
    
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
}
