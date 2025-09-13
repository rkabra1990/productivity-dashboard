package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/habits")
public class HabitController {

    @Autowired 
    private HabitService habitService;

    @GetMapping
    public String viewHabits(Model model) {
        // Get all active (non-archived) habits
        List<Habit> habits = habitService.getAllHabits();
        
        // Get today's habits
        List<Habit> todaysHabits = habitService.getTodaysHabits();
        
        // Get habit statistics
        Map<String, Object> stats = habitService.getHabitStats();
        
        // Add attributes to the model
        model.addAttribute("habits", habits);
        model.addAttribute("todaysHabits", todaysHabits);
        model.addAttribute("stats", stats);
        model.addAttribute("habit", new Habit());
        
        // Add enums for the form
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("recurrences", Recurrence.values());
        model.addAttribute("daysOfWeek", DayOfWeek.values());
        
        return "habits";
    }
    
    @GetMapping("/archived")
    public String viewArchivedHabits(Model model) {
        model.addAttribute("habits", habitService.getArchivedHabits());
        return "habits-archived";
    }

    @PostMapping
    public String addHabit(@ModelAttribute Habit habit, 
                          @RequestParam(required = false) Integer dayOfWeek,
                          @RequestParam(required = false) Integer dayOfMonth,
                          @RequestParam(required = false) Integer month) {
        
        // Set recurrence-specific fields
        if (habit.getRecurrence() == Recurrence.WEEKLY && dayOfWeek != null) {
            habit.setWeeklyDay(DayOfWeek.of(dayOfWeek));
        } else if (habit.getRecurrence() == Recurrence.MONTHLY && dayOfMonth != null) {
            habit.setMonthlyDay(dayOfMonth);
        } else if (habit.getRecurrence() == Recurrence.YEARLY && month != null && dayOfMonth != null) {
            habit.setYearlyMonth(month);
            habit.setYearlyDay(dayOfMonth);
        }
        
        habitService.createHabit(habit);
        return "redirect:/habits";
    }
    
    @PostMapping("/{id}/skip")
    @ResponseBody
    public ResponseEntity<?> skipHabit(@PathVariable Long id) {
        try {
            habitService.skipHabit(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error skipping habit: " + e.getMessage());
        }
    }
    
    @PostMapping("/{habitId}/complete/{logId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeHabit(
            @PathVariable Long habitId,
            @PathVariable Long logId) {
        try {
            HabitLog log = habitService.completeHabit(habitId, logId);
            Habit habit = log.getHabit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentStreak", habit.getCurrentStreak());
            response.put("bestStreak", habit.getBestStreak());
            
            // Add next occurrence to response
            LocalDateTime nextOccurrence = habit.getNextOccurrence();
            response.put("nextOccurrence", nextOccurrence.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("nextOccurrenceFormatted", 
                nextOccurrence.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")));
                
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/archive")
    public String archiveHabit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            habitService.archiveHabit(id);
            redirectAttributes.addFlashAttribute("message", "Habit archived successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error archiving habit: " + e.getMessage());
        }
        return "redirect:/habits";
    }
    
    @PostMapping("/{id}/unarchive")
    public String unarchiveHabit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            habitService.unarchiveHabit(id);
            redirectAttributes.addFlashAttribute("message", "Habit unarchived successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error unarchiving habit: " + e.getMessage());
        }
        return "redirect:/habits/archived";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteHabit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            habitService.deleteHabit(id);
            redirectAttributes.addFlashAttribute("message", "Habit deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting habit: " + e.getMessage());
        }
        return "redirect:/habits";
    }
    
    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getHabitStats() {
        return habitService.getHabitStats();
    }
}
