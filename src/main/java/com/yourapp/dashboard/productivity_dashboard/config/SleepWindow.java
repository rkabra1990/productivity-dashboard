package com.yourapp.dashboard.productivity_dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "sleep")
public class SleepWindow {
    private String start; // e.g. "23:00"
    private String end;   // e.g. "06:00"
    private LocalTime sleepStart;
    private LocalTime sleepEnd;

    @PostConstruct
    public void init() {
        sleepStart = LocalTime.parse(start);
        sleepEnd = LocalTime.parse(end);
    }

    public static boolean isAsleep(LocalTime now, LocalTime sleepStart, LocalTime sleepEnd) {
        if (sleepStart.isBefore(sleepEnd)) {
            return now.isAfter(sleepStart) && now.isBefore(sleepEnd);
        } else {
            return now.isAfter(sleepStart) || now.isBefore(sleepEnd);
        }
    }

    // Getters and setters for configuration
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    public LocalTime getSleepStart() { return sleepStart; }
    public LocalTime getSleepEnd() { return sleepEnd; }
}
