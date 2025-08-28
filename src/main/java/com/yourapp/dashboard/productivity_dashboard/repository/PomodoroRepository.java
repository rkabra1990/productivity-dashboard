package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.PomodoroSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PomodoroRepository extends JpaRepository<PomodoroSession, Long> {
    @Query("SELECT FUNCTION('DAYOFWEEK', p.startTime) as day, SUM(p.sessions * 25) FROM PomodoroSession p " +
            "WHERE p.startTime BETWEEN :start AND :end GROUP BY FUNCTION('DAYOFWEEK', p.startTime)")
    List<Object[]> sumPomodoroMinutesByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}


