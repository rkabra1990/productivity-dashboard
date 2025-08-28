package com.yourapp.dashboard.productivity_dashboard.model;

import com.yourapp.dashboard.productivity_dashboard.service.Priority;

import java.time.LocalDateTime;

public class MatrixItem {
    private String title;
    private Priority priority;
    private LocalDateTime dueDate;

    public MatrixItem(String title, Priority priority, LocalDateTime dueDate) {
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    // Getters
    public String getTitle() { return title; }
    public Priority getPriority() { return priority; }
    public LocalDateTime getDueDate() { return dueDate; }
}

