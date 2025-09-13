package com.yourapp.dashboard.productivity_dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
public class HabitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDateTime scheduledDateTime;
    private LocalDateTime completedDateTime;
    private LocalDateTime missedDateTime;
    private Boolean completed = false;
    private Boolean missed = false;
    private Boolean notifSent = false;
    private Boolean gracePeriodUsed = false;
    private String timeZone; // Store timezone at the time of scheduling
    
    // For tracking completion within grace period
    private Boolean completedInGracePeriod = false;
    
    // For tracking skipped habits
    private Boolean skipped = false;
    
    // For tracking rescheduled habits
    private Boolean rescheduled = false;
    private String rescheduleReason;
    
    private Boolean error = false;
    private String errorMessage;

    public HabitLog(Object o, Habit habit, LocalDateTime localDateTime, Boolean completed) {
        this.habit = habit;
        this.scheduledDateTime = localDateTime;
        this.completed = completed != null ? completed : false;
    }

    public HabitLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Habit getHabit() {
        return habit;
    }

    public void setHabit(Habit habit) {
        this.habit = habit;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public Boolean getCompleted() {
        return completed != null ? completed : false;
    }
    
    // Keep isCompleted() for backward compatibility
    public Boolean isCompleted() {
        return getCompleted();
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed != null ? completed : false;
    }

    public Boolean getNotifSent() {
        return notifSent != null ? notifSent : false;
    }

    public void setNotifSent(Boolean notifSent) {
        this.notifSent = notifSent != null ? notifSent : false;
    }

    public LocalDateTime getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(LocalDateTime completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public LocalDateTime getMissedDateTime() {
        return missedDateTime;
    }

    public void setMissedDateTime(LocalDateTime missedDateTime) {
        this.missedDateTime = missedDateTime;
    }

    public Boolean getMissed() {
        return missed != null ? missed : false;
    }

    public void setMissed(Boolean missed) {
        this.missed = missed != null ? missed : false;
    }

    public Boolean getGracePeriodUsed() {
        return gracePeriodUsed != null ? gracePeriodUsed : false;
    }

    public void setGracePeriodUsed(Boolean gracePeriodUsed) {
        this.gracePeriodUsed = gracePeriodUsed != null ? gracePeriodUsed : false;
    }

    public String getTimeZone() {
        return timeZone != null ? timeZone : ZoneId.systemDefault().toString();
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getCompletedInGracePeriod() {
        return completedInGracePeriod != null ? completedInGracePeriod : false;
    }

    public void setCompletedInGracePeriod(Boolean completedInGracePeriod) {
        this.completedInGracePeriod = completedInGracePeriod != null ? completedInGracePeriod : false;
    }

    public Boolean getRescheduled() {
        return rescheduled != null ? rescheduled : false;
    }

    public void setRescheduled(Boolean rescheduled) {
        this.rescheduled = rescheduled != null ? rescheduled : false;
    }

    public String getRescheduleReason() {
        return rescheduleReason;
    }
    
    public Boolean getError() {
        return error;
    }
    
    public void setError(Boolean error) {
        this.error = error;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setRescheduleReason(String rescheduleReason) {
        this.rescheduleReason = rescheduleReason;
    }
    
    public Boolean isSkipped() {
        return skipped != null ? skipped : false;
    }
    
    public void setSkipped(Boolean skipped) {
        this.skipped = skipped != null ? skipped : false;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timeZone == null) {
            this.timeZone = ZoneId.systemDefault().toString();
        }
    }

    @Override
    public String toString() {
        return "HabitLog{" +
                "id=" + id +
                ", habit=" + (habit != null ? habit.getId() : "null") +
                ", scheduledDateTime=" + scheduledDateTime +
                ", completed=" + completed +
                ", missed=" + missed +
                ", gracePeriodUsed=" + gracePeriodUsed +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }
}
