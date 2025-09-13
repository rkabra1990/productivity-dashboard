package com.yourapp.dashboard.productivity_dashboard.model;

/**
 * Defines the recurrence pattern for habits.
 */
public enum Recurrence {
    /**
     * Repeats every hour
     */
    HOURLY,
    
    /**
     * Repeats every day
     */
    DAILY,
    
    /**
     * Repeats every week on a specific day
     */
    WEEKLY,
    
    /**
     * Repeats every month on a specific day
     */
    MONTHLY,
    
    /**
     * Repeats every year on a specific date
     */
    YEARLY
}
