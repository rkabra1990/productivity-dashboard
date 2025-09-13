package com.yourapp.dashboard.productivity_dashboard.model;

import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import jakarta.persistence.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Habit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitLog> logs = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    private Recurrence recurrence; // HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY

    // Optional fields depending on recurrence
    @Enumerated(EnumType.STRING)
    private DayOfWeek weeklyDay;        // for WEEKLY recurrence

    private Integer monthlyDay;                   // 1-31 for MONTHLY recurrence

    private Integer yearlyMonth;                  // 1-12 for YEARLY recurrence
    private Integer yearlyDay;                    // 1-31 for YEARLY recurrence

    private Boolean archived = false;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private int missedCount = 0;
    private LocalDate lastCompleted;
    private LocalDateTime lastScheduled;
    private LocalDateTime nextScheduled;
    private Integer gracePeriodMinutes = 15; // Default 15-minute grace period
    private boolean allowMultipleDaily = false;
    private List<LocalTime> dailyReminderTimes = new ArrayList<>(); // For multiple daily reminders
    private String timeZone = ZoneId.systemDefault().toString(); // Store user's timezone
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public DayOfWeek getWeeklyDay() {
        return weeklyDay;
    }

    public void setWeeklyDay(DayOfWeek weeklyDay) {
        this.weeklyDay = weeklyDay;
    }

    public Integer getMonthlyDay() {
        return monthlyDay;
    }

    public void setMonthlyDay(Integer monthlyDay) {
        this.monthlyDay = monthlyDay;
    }

    public Integer getYearlyMonth() {
        return yearlyMonth;
    }

    public void setYearlyMonth(Integer yearlyMonth) {
        this.yearlyMonth = yearlyMonth;
    }

    public Integer getYearlyDay() {
        return yearlyDay;
    }

    public Boolean getArchived() {
        return archived != null ? archived : false;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived != null ? archived : false;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        if (currentStreak > this.bestStreak) {
            this.bestStreak = currentStreak;
        }
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public LocalDate getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(LocalDate lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public void setYearlyDay(Integer yearlyDay) {
        this.yearlyDay = yearlyDay;
    }
    
    public Integer getGracePeriodMinutes() {
        return gracePeriodMinutes != null ? gracePeriodMinutes : 15; // Default to 15 minutes if null
    }
    
    public void setGracePeriodMinutes(Integer gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes != null ? gracePeriodMinutes : 15; // Ensure never null
    }
    
    public boolean isAllowMultipleDaily() {
        return allowMultipleDaily;
    }
    
    public void setAllowMultipleDaily(boolean allowMultipleDaily) {
        this.allowMultipleDaily = allowMultipleDaily;
    }
    
    public LocalDateTime getLastScheduled() {
        return lastScheduled;
    }
    
    public void setLastScheduled(LocalDateTime lastScheduled) {
        this.lastScheduled = lastScheduled;
    }
    
    public int getMissedCount() {
        return missedCount;
    }
    
    public void setMissedCount(int missedCount) {
        this.missedCount = missedCount;
    }
    
    public String getTimeZone() {
        return timeZone != null ? timeZone : ZoneId.systemDefault().toString();
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone != null ? timeZone : ZoneId.systemDefault().toString();
    }
    
    @ElementCollection
    @CollectionTable(name = "habit_reminder_times", joinColumns = @JoinColumn(name = "habit_id"))
    @Column(name = "reminder_time")
    public List<LocalTime> getDailyReminderTimes() {
        if (dailyReminderTimes == null) {
            dailyReminderTimes = new ArrayList<>();
        }
        return dailyReminderTimes;
    }
    
    public void setDailyReminderTimes(List<LocalTime> dailyReminderTimes) {
        this.dailyReminderTimes = dailyReminderTimes != null ? 
            new ArrayList<>(dailyReminderTimes) : new ArrayList<>();
    }
    
    public List<HabitLog> getLogs() {
        return logs;
    }
    
    public void setLogs(List<HabitLog> logs) {
        this.logs = logs;
    }
    
    public void addLog(HabitLog log) {
        logs.add(log);
        log.setHabit(this);
    }
    
    public void removeLog(HabitLog log) {
        logs.remove(log);
        log.setHabit(null);
    }
    
    @Transient
    public double getCompletionRate() {
        if (logs == null || logs.isEmpty()) {
            return 0.0;
        }
        
        long totalLogs = logs.size();
        long completedLogs = logs.stream()
            .filter(log -> log != null && Boolean.TRUE.equals(log.isCompleted()))
            .count();
        
        return totalLogs > 0 ? (completedLogs * 100.0) / totalLogs : 0.0;
    }
    
    @Transient
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
    
    public LocalDateTime getNextOccurrence() {
        if (this.recurrence == null) {
            return LocalDateTime.now().plusDays(1); // Default to daily if recurrence is not set
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime scheduledTime = this.scheduledTime != null ? this.scheduledTime : LocalTime.NOON;
        
        try {
            switch (this.recurrence) {
                case HOURLY:
                    return now.plusHours(1).withMinute(scheduledTime.getMinute())
                             .withSecond(0).withNano(0);
                             
                case DAILY:
                    LocalDateTime nextDaily = LocalDateTime.of(now.toLocalDate(), scheduledTime);
                    return nextDaily.isAfter(now) ? nextDaily : nextDaily.plusDays(1);
                    
                case WEEKLY:
                    LocalDateTime nextWeekly = LocalDateTime.of(now.toLocalDate(), scheduledTime);
                    int targetDay = this.weeklyDay != null ? this.weeklyDay.getValue() : now.getDayOfWeek().getValue();
                    int daysUntilNext = (targetDay - now.getDayOfWeek().getValue() + 7) % 7;
                    daysUntilNext = daysUntilNext == 0 ? 7 : daysUntilNext; // If same day but time has passed, schedule for next week
                    return nextWeekly.plusDays(daysUntilNext);
                    
                case MONTHLY:
                    int dayOfMonth = this.monthlyDay != null ? this.monthlyDay : now.getDayOfMonth();
                    LocalDateTime nextMonthly = LocalDateTime.of(now.getYear(), now.getMonth(), 
                        Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth()), 
                        scheduledTime.getHour(), scheduledTime.getMinute());
                    if (nextMonthly.isAfter(now)) {
                        return nextMonthly;
                    }
                    LocalDateTime nextMonth = nextMonthly.plusMonths(1);
                    return nextMonth.withDayOfMonth(
                        Math.min(dayOfMonth, nextMonth.toLocalDate().lengthOfMonth()));
                        
                case YEARLY:
                    int month = this.yearlyMonth != null ? this.yearlyMonth : now.getMonthValue();
                    int day = this.yearlyDay != null ? this.yearlyDay : now.getDayOfMonth();
                    YearMonth yearMonth = YearMonth.of(now.getYear(), month);
                    LocalDateTime nextYearly = LocalDateTime.of(now.getYear(), month, 
                        Math.min(day, yearMonth.lengthOfMonth()),
                        scheduledTime.getHour(), scheduledTime.getMinute());
                    if (nextYearly.isAfter(now)) {
                        return nextYearly;
                    }
                    return nextYearly.plusYears(1);
                    
                default:
                    return now.plusDays(1); // Fallback to daily
            }
        } catch (Exception e) {
            return now.plusDays(1); // Fallback to daily in case of any error
        }
    }
}
