package com.yourapp.dashboard.productivity_dashboard.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class PomodoroSession {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    private Task task;

    @ManyToOne
    private Habit habit;

    private int sessions =1;
    private LocalDateTime startTime;
    private boolean breakTaken;


    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Habit getHabit() {
        return habit;
    }

    public void setHabit(Habit habit) {
        this.habit = habit;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSessions() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public boolean isBreakTaken() {
        return breakTaken;
    }

    public void setBreakTaken(boolean breakTaken) {
        this.breakTaken = breakTaken;
    }
}

