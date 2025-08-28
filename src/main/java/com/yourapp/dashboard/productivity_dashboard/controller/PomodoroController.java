package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.PomodoroSession;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import com.yourapp.dashboard.productivity_dashboard.service.PomodoroService;
import com.yourapp.dashboard.productivity_dashboard.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/pomodoro")
public class PomodoroController {
    @Autowired
    PomodoroService service;

    @Autowired
    TaskService taskService;

    @Autowired
    HabitService habitService;

    @GetMapping
    public String view(Model model) {
        model.addAttribute("pomodoroSession", new PomodoroSession());
        model.addAttribute("tasks", taskService.getPendingTasks());
        model.addAttribute("habits", habitService.getAllHabits());
        return "pomodoro";
    }


    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<String> add(@RequestParam(required = false) Long taskId,
                      @RequestParam(required = false) Long habitId
    ) {
        PomodoroSession session = new PomodoroSession();
        session.setStartTime(LocalDateTime.now());
        session.setSessions(1);
        session.setBreakTaken(false);

        if (taskId != null) {
            service.findByTaskId(taskId).ifPresent(session::setTask);
        } else if (habitId != null) {
            service.findByHabitId(habitId).ifPresent(session::setHabit);
        }

        service.save(session);
        return ResponseEntity.ok("Pomodoro session saved successfully.");
    }
}
