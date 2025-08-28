package com.yourapp.dashboard.productivity_dashboard.scheduler;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HabitScheduler {

    private final HabitRepository habitRepo;
    private final HabitService habitService;

    // Runs every hour to ensure at least one upcoming log exists for each habit
    @Scheduled(cron = "0 0 * * * *")
    public void ensureUpcomingLogs() {
        List<Habit> habits = habitRepo.findAll();
        for (Habit habit : habits) {
            habitService.generateTodayLogsForHabit(habit);
        }
    }
}
