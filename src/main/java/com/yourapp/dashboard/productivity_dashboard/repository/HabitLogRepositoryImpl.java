package com.yourapp.dashboard.productivity_dashboard.repository;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class HabitLogRepositoryImpl implements HabitLogRepositoryCustom {
    private static final Logger logger = LoggerFactory.getLogger(HabitLogRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<HabitLog> findLogsByHabitAndDateRange(Habit habit, LocalDateTime start, LocalDateTime end) {
        try {
            String jpql = "SELECT hl FROM HabitLog hl WHERE hl.habit = :habit AND hl.scheduledDateTime BETWEEN :start AND :end";
            TypedQuery<HabitLog> query = entityManager.createQuery(jpql, HabitLog.class)
                .setParameter("habit", habit)
                .setParameter("start", start)
                .setParameter("end", end);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding logs by habit and date range", e);
            throw e;
        } finally {
            // Ensure the EntityManager is cleared to prevent memory leaks
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.clear();
            }
        }
    }

    @Override
    @Transactional
    public void markLogsAsProcessed(List<Long> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            return;
        }
        
        try {
            String jpql = "UPDATE HabitLog hl SET hl.processed = true WHERE hl.id IN :ids";
            entityManager.createQuery(jpql)
                .setParameter("ids", logIds)
                .executeUpdate();
            entityManager.flush();
        } catch (Exception e) {
            logger.error("Error marking logs as processed", e);
            throw e;
        }
    }
}
