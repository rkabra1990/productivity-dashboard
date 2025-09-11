package com.yourapp.dashboard.productivity_dashboard.model;

import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;
    @Enumerated(EnumType.STRING)
    private Priority priority; // LOW, MEDIUM, HIGH
    private LocalDateTime dueDate;
    private boolean completed;

    private boolean notifSent;
    
    @Column(name = "completion_timestamp")
    private LocalDateTime completionTimestamp;

    // ✅ Getters & Setters
    // If using Lombok: @Getter @Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isNotifSent() {
        return notifSent;
    }

    public void setNotifSent(boolean notifSent) {
        this.notifSent = notifSent;
    }

    public LocalDateTime getCompletionTimestamp() {
        return completionTimestamp;
    }

    public void setCompletionTimestamp(LocalDateTime completionTimestamp) {
        this.completionTimestamp = completionTimestamp;
    }
}
