package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import com.yourapp.dashboard.productivity_dashboard.service.PomodoroService;
import com.yourapp.dashboard.productivity_dashboard.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private TaskService taskService;
    @Autowired private HabitService habitService;
    @Autowired private PomodoroService pomodoroService;

    @GetMapping
    public String viewDashboard(Model model) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY);
        LocalDateTime endOfDay = LocalDateTime.of(today.toLocalDate(), LocalTime.MAX);
        Map<String, Long> stats = taskService.getTodayTaskStats();
        model.addAttribute("completedCount", stats.get("completed"));
        model.addAttribute("pendingCount", stats.get("pending"));
        model.addAttribute("todayTasks", taskService.findTasksForToday());
        List<Habit> todayHabits = habitService.getTodayHabits();
        Map<Long, Boolean> habitStatus = new HashMap<>();

        for (Habit habit : todayHabits) {
            habitStatus.put(habit.getId(), habitService.isCompletedToday(habit));
        }

        model.addAttribute("todayHabits", todayHabits);
        model.addAttribute("habitStatus", habitStatus);
        model.addAttribute("todayPomodoro", pomodoroService.getTodayPomodoroMinutes());
        model.addAttribute("completedTasks", taskService.getCompletedTasks());
        model.addAttribute("pendingTasks", taskService.getPendingTasks());
        model.addAttribute("weeklyTasks", taskService.countCompletedTasksByDay(weekStart, weekEnd));
        model.addAttribute("weeklyPomodoro", pomodoroService.sumPomodoroMinutesByDay(weekStart, weekEnd));
        return "dashboard";
    }
}
