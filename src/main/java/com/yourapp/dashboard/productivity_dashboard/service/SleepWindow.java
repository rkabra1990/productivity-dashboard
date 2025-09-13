package com.yourapp.dashboard.productivity_dashboard.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Manages sleep window configuration to avoid scheduling notifications during sleep hours.
 */
@Component
public class SleepWindow {
    private LocalTime sleepStart = LocalTime.of(22, 0); // 10:00 PM
    private LocalTime sleepEnd = LocalTime.of(7, 0);    // 7:00 AM

    /**
     * Checks if the given time is within sleep hours.
     * @param time The time to check
     * @param sleepStart Start of sleep window
     * @param sleepEnd End of sleep window
     * @return true if the time is within sleep hours, false otherwise
     */
    public static boolean isSleepTime(LocalTime time, LocalTime sleepStart, LocalTime sleepEnd) {
        if (time == null) {
            return false;
        }
        
        if (sleepStart.isBefore(sleepEnd)) {
            // Sleep window doesn't cross midnight
            return !time.isBefore(sleepStart) && !time.isAfter(sleepEnd);
        } else {
            // Sleep window crosses midnight
            return !time.isBefore(sleepStart) || !time.isAfter(sleepEnd);
        }
    }

    /**
     * Checks if the current time is within sleep hours.
     * @return true if current time is within sleep hours, false otherwise
     */
    public boolean isSleepTime() {
        return isSleepTime(LocalTime.now(), sleepStart, sleepEnd);
    }

    public LocalTime getSleepStart() {
        return sleepStart;
    }

    public void setSleepStart(LocalTime sleepStart) {
        this.sleepStart = sleepStart != null ? sleepStart : LocalTime.of(22, 0);
    }

    public LocalTime getSleepEnd() {
        return sleepEnd;
    }

    public void setSleepEnd(LocalTime sleepEnd) {
        this.sleepEnd = sleepEnd != null ? sleepEnd : LocalTime.of(7, 0);
    }
}
