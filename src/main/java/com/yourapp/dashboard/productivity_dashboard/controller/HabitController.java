package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import com.yourapp.dashboard.productivity_dashboard.model.Recurrence;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final HabitService habitService;
    private final HabitLogRepository logRepo;

    @Autowired
    public HabitController(HabitService habitService, HabitLogRepository logRepo) {
        this.habitService = habitService;
        this.logRepo = logRepo;
    }

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
    
    @GetMapping("/skip/{id}")
    @Transactional
    public String skipHabit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            // Get the current date's log for this habit
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            // Find the habit with the ID
            Habit habit = habitService.getHabitById(id)
                .orElseThrow(() -> new RuntimeException("Habit not found with id: " + id));
                
            // Find today's log for this habit
            List<HabitLog> todayLogs = logRepo.findByHabitAndScheduledDateTimeBetween(
                habit, startOfDay, endOfDay);
                
            HabitLog log;
            if (todayLogs.isEmpty()) {
                // If no log exists for today, create one
                log = new HabitLog();
                log.setHabit(habit);
                log.setScheduledDateTime(LocalDateTime.now());
            } else {
                log = todayLogs.get(0);
            }
            
            // Mark as skipped if not already completed
            if (!log.getCompleted()) {
                log.setSkipped(true);
                log.setMissedDateTime(LocalDateTime.now());
                log = logRepo.save(log);
                
                // Update habit streaks
                habitService.updateHabitStreaks(habit);
                
                redirectAttributes.addFlashAttribute("successMessage", "Habit marked as skipped!");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage", "Habit was already completed for today!");
            }
            
            return "redirect:/habits";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to skip habit: " + e.getMessage());
            return "redirect:/habits";
        }
    }
    
    @GetMapping("/complete/{habitId}")
    @Transactional
    public String completeHabit(
            @PathVariable Long habitId,
            @RequestParam(required = false) Long logId,
            RedirectAttributes redirectAttributes) {
        try {
            Habit habit = habitService.getHabitById(habitId)
                .orElseThrow(() -> new RuntimeException("Habit not found with id: " + habitId));
            
            if (logId != null) {
                // Complete specific log (for hourly habits)
                HabitLog log = logRepo.findById(logId)
                    .orElseThrow(() -> new RuntimeException("Log not found with id: " + logId));
                
                if (!log.getHabit().getId().equals(habitId)) {
                    throw new RuntimeException("Log does not belong to the specified habit");
                }
                
                if (!log.getCompleted()) {
                    log.setCompleted(true);
                    log.setCompletedDateTime(LocalDateTime.now());
                    logRepo.save(log);
                    
                    // Update habit streaks
                    habitService.updateHabitStreaks(habit);
                    
                    redirectAttributes.addFlashAttribute("successMessage", "Habit marked as completed for " + 
                        log.getScheduledDateTime().format(DateTimeFormatter.ofPattern("h:mma")) + "!");
                } else {
                    redirectAttributes.addFlashAttribute("infoMessage", "This habit was already completed!");
                }
            } else {
                // For non-hourly habits
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                
                // Find or create today's log for this habit
                List<HabitLog> todayLogs = logRepo.findByHabitAndScheduledDateTimeBetween(
                    habit, startOfDay, endOfDay);
                
                HabitLog log;
                if (todayLogs.isEmpty()) {
                    log = new HabitLog();
                    log.setHabit(habit);
                    log.setScheduledDateTime(now);
                } else {
                    log = todayLogs.get(0);
                }
                
                if (!log.getCompleted()) {
                    log.setCompleted(true);
                    log.setCompletedDateTime(now);
                    logRepo.save(log);
                    
                    // Update habit streaks
                    habitService.updateHabitStreaks(habit);
                    
                    redirectAttributes.addFlashAttribute("successMessage", "Habit marked as completed!");
                } else {
                    redirectAttributes.addFlashAttribute("infoMessage", "Habit was already completed for today!");
                }
            }
            
            return "redirect:/habits";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete habit: " + e.getMessage());
            return "redirect:/habits";
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
