package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDueDate(LocalDateTime now);

    List<Task> findByCompleted(boolean completed);

    @Query("SELECT FUNCTION('DAYOFWEEK', t.dueDate) as day, COUNT(t) FROM Task t " +
            "WHERE t.completed = true AND t.dueDate BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('DAYOFWEEK', t.dueDate)")
    List<Object[]> countCompletedTasksByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Task> findByDueDateBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Task> findByCompletedTrueAndDueDateAfter(LocalDateTime oneYearAgo);

    List<Task> findByDueDateBefore(LocalDateTime oneYearAgo);

    List<Task> findByCompletedFalseAndNotifSentFalseAndDueDateBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT DISTINCT YEAR(t.dueDate) FROM Task t ORDER BY YEAR(t.dueDate) DESC")
    List<Integer> findDistinctYears();
}
