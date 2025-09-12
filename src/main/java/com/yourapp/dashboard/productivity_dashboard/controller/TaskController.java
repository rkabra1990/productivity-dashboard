package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.Task;
import com.yourapp.dashboard.productivity_dashboard.service.TaskService;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;

@Controller
public class TaskController {
    @Autowired
    private TaskService service;

    @GetMapping("/tasks/test")
    public String testPage(Model model) {
        model.addAttribute("testMessage", "Test page is working!");
        return "test";
    }
    
    @GetMapping("/tasks/simple")
    public String simpleTasksPage(Model model) {
        model.addAttribute("testMessage", "Simple tasks page is working!");
        model.addAttribute("tasks", service.getAllTasks());
        return "tasks-simple";
    }
    
    @GetMapping("/tasks")
    public String viewTasks(@RequestParam(value = "year", required = false) Integer year, 
                          Model model, 
                          HttpServletRequest request) {
        // Add CSRF token to model
        HttpSession session = request.getSession(false);
        if (session != null) {
            org.springframework.security.web.csrf.CsrfToken csrfToken = 
                (org.springframework.security.web.csrf.CsrfToken) request.getAttribute("_csrf");
            if (csrfToken != null) {
                model.addAttribute("_csrf", csrfToken);
            }
        }
        
        List<Integer> years = service.getAvailableYears();

        int selectedYear;
        if (year != null && years.contains(year)) {
            selectedYear = year;
        } else if (!years.isEmpty()) {
            selectedYear = years.get(0); // fallback to first available year
        } else {
            selectedYear = LocalDate.now().getYear(); // no data at all
        }
        
        // Get completed tasks for the selected year
        Map<Month, List<Task>> tasksByMonth = service.getTasksByYearGroupedByMonth(selectedYear);
        
        // Get tasks
        List<Task> pendingTasks = service.getPendingTasks();
        List<Task> completedTasks = service.getCompletedTasks();
        
        // Get completed tasks grouped by month
        Map<String, List<Task>> completedTasksByMonth = service.getCompletedTasksGroupedByMonth();

        // CSRF token is automatically added by Thymeleaf

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("tasksByMonth", completedTasksByMonth);
        model.addAttribute("years", service.getAvailableYears());
        model.addAttribute("task", new Task());
        model.addAttribute("tasks", pendingTasks);
        model.addAttribute("completedTasksCount", completedTasks.size());

        return "tasks";
    }

    @PostMapping("/tasks/add")
    public String addTask(
            @RequestParam String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setCategory(category);
        task.setPriority(priority);
        task.setDueDate(dueDate != null ? dueDate : LocalDateTime.now());
        task.setCompleted(false);
        service.saveTask(task);
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id) {
        service.deleteTask(id);
        return "redirect:/tasks";
    }
    @PostMapping("/tasks/toggle")
    public String toggleTaskCompletion(@RequestParam Long id) {
        service.toggleCompletion(id);
        return "redirect:/tasks";
    }
    @PostMapping("/tasks/update")
    public String updateTask(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate) {
        Task task = service.getTaskById(id);
        if (task != null) {
            task.setTitle(title);
            task.setCategory(category);
            task.setPriority(priority);
            task.setDueDate(dueDate);
            service.saveTask(task);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/delete-year")
    public String deleteTasksByYear(@RequestParam("year") int year, RedirectAttributes redirectAttributes) {
        service.deleteTasksByYear(year);
        redirectAttributes.addFlashAttribute("message", "Tasks from " + year + " deleted successfully.");
        return "redirect:/tasks?year=" + year;
    }
    @GetMapping("/tasks/export-year")
    public void exportTasksForYear(@RequestParam("year") int year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=tasks_" + year + ".xls");

        service.exportTasksForYearToExcel(year, response.getOutputStream());
    }



}

