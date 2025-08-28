package com.yourapp.dashboard.productivity_dashboard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class HabitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Habit habit;

    private LocalDateTime scheduledDateTime;
    private Boolean completed = false;
    private Boolean notifSent = false;

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
}
