package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import com.yourapp.dashboard.productivity_dashboard.service.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByRecurrence(Recurrence recurrence);
    
    // Find habits by archived status
    List<Habit> findByArchivedFalse();
    List<Habit> findByArchivedTrue();
    
    // Find habits by priority
    List<Habit> findByPriority(Priority priority);
    
    // Find habits by recurrence and archived status
    @Query("SELECT h FROM Habit h WHERE h.recurrence = :recurrence AND h.archived = false")
    List<Habit> findByRecurrenceAndArchivedFalse(@Param("recurrence") Recurrence recurrence);
    
    // Find habits that were last completed before a certain date
    List<Habit> findByLastCompletedBefore(LocalDate date);
    
    // Find habits with current streak greater than or equal to a value
    List<Habit> findByCurrentStreakGreaterThanEqual(int streak);
    
    // Find habits with best streak greater than or equal to a value
    List<Habit> findByBestStreakGreaterThanEqual(int streak);
    
    // Find habits that need to be completed today
    @Query("SELECT h FROM Habit h WHERE h.archived = false AND " +
           "(h.recurrence = 'DAILY' OR " +
           "(h.recurrence = 'WEEKLY' AND h.weeklyDay = :dayOfWeek) OR " +
           "(h.recurrence = 'MONTHLY' AND h.monthlyDay = :dayOfMonth) OR " +
           "(h.recurrence = 'YEARLY' AND h.yearlyMonth = :month AND h.yearlyDay = :dayOfMonth))")
    List<Habit> findTodaysHabits(
        @Param("dayOfWeek") int dayOfWeek,
        @Param("dayOfMonth") int dayOfMonth,
        @Param("month") int month
    );
    
    // Count habits by completion status for today
    @Query("SELECT COUNT(DISTINCT h) FROM Habit h JOIN h.logs l WHERE " +
           "h.archived = false AND " +
           "FUNCTION('DATE', l.scheduledDateTime) = CURRENT_DATE AND " +
           "l.completed = :completed")
    long countByCompletedToday(@Param("completed") boolean completed);
}
