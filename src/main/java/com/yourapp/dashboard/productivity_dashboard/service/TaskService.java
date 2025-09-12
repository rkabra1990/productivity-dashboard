package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.model.MatrixItem;
import com.yourapp.dashboard.productivity_dashboard.model.Task;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.TaskRepository;
import jakarta.servlet.ServletOutputStream;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {
    @Autowired
    private TaskRepository repo;

    @Autowired
    HabitRepository habitRepo;

    @Autowired
    private HabitService habitService;

    public List<Task> getAllTasks() { return repo.findAll(); }
    public void saveTask(Task task) { task.setCompleted(false);repo.save(task); }
    
    public Task getTaskById(Long id) {
        return repo.findById(id).orElse(null);
    }


    public void deleteTask(Long id) {
        repo.deleteById(id);
    }

    public List<Task> findTasksForToday() {
        return repo.findByDueDate(LocalDateTime.now());
    }
    
    public List<Integer> getAvailableYears() {
        return repo.findDistinctYears();
    }
    
    public Map<Month, List<Task>> getTasksByYearGroupedByMonth(int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59);
        List<Task> tasks = repo.findByDueDateBetween(startOfYear, endOfYear);
        
        return tasks.stream()
            .collect(Collectors.groupingBy(
                task -> task.getDueDate().getMonth()
            ));
    }
    
    public List<Task> getPendingTasks() {
        return repo.findByCompleted(false);
    }
    
    public List<Task> getCompletedTasks() {
        return repo.findByCompleted(true);
    }
    
    public Map<String, List<Task>> getCompletedTasksGroupedByMonth() {
        List<Task> completedTasks = repo.findByCompleted(true);
        Map<String, List<Task>> tasksByMonth = new TreeMap<>(Collections.reverseOrder());
        
        for (Task task : completedTasks) {
            LocalDateTime timestamp = task.getCompletionTimestamp();
            // If completion timestamp is null but task is marked as completed, use current time
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
                task.setCompletionTimestamp(timestamp);
                repo.save(task); // Update the task with the new timestamp
            }
            
            String monthYear = timestamp.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            tasksByMonth.computeIfAbsent(monthYear, k -> new ArrayList<>()).add(task);
        }
        
        // Sort tasks within each month by completion timestamp (newest first)
        tasksByMonth.forEach((month, tasks) -> {
            tasks.sort((t1, t2) -> t2.getCompletionTimestamp().compareTo(t1.getCompletionTimestamp()));
        });
        
        return tasksByMonth;
    }
    
    public Map<String, Long> getTodayTaskStats() {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Task> todayTasks = repo.findByDueDateBetween(startOfDay, endOfDay);

        long completed = todayTasks.stream().filter(Task::isCompleted).count();
        long pending = todayTasks.size() - completed;

        Map<String, Long> result = new HashMap<>();
        result.put("completed", completed);
        result.put("pending", pending);
        return result;
    }
    public void toggleCompletion(Long id) {
        Optional<Task> taskOpt = repo.findById(id);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            boolean newCompletedState = !task.isCompleted();
            task.setCompleted(newCompletedState);
            // Set completion timestamp when marking as completed
            if (newCompletedState && task.getCompletionTimestamp() == null) {
                task.setCompletionTimestamp(LocalDateTime.now());
            } else if (!newCompletedState) {
                task.setCompletionTimestamp(null);
            }
            repo.save(task);
        }
    }

    public long[] countCompletedTasksByDay(LocalDateTime start, LocalDateTime end) {
        long[] weeklyTasks = new long[7];
        repo.countCompletedTasksByDay(start, end)
                .forEach(row -> weeklyTasks[((int) row[0]) - 1] = (long) row[1]);
        return weeklyTasks;
    }
    public Map<String, List<MatrixItem>> getMatrixItems() {
        LocalDateTime now = LocalDateTime.now();

        // Get incomplete tasks
        List<Task> tasks = repo.findAll().stream()
                .filter(t -> !t.isCompleted() && t.getDueDate() != null)
                .toList();

        // Get habits
        List<Habit> habits = habitService.getAllHabits();

        // Prepare unified task+habit view model
        List<MatrixItem> allItems = new ArrayList<>();

        // Convert tasks
        for (Task task : tasks) {
            allItems.add(new MatrixItem(task.getTitle(), task.getPriority(), task.getDueDate()));
        }

        // Convert habits using next upcoming log date
        for (Habit habit : habits) {
            List<HabitLog> upcoming = habitService.getUpcomingLogs(habit);
            if (!upcoming.isEmpty()) {
                LocalDateTime due = upcoming.get(0).getScheduledDateTime();
                allItems.add(new MatrixItem(habit.getName(), habit.getPriority(), due));
            }
        }

        // Now sort into matrix buckets
        List<MatrixItem> doFirst = new ArrayList<>();
        List<MatrixItem> schedule = new ArrayList<>();
        List<MatrixItem> delegate = new ArrayList<>();
        List<MatrixItem> eliminate = new ArrayList<>();

        for (MatrixItem item : allItems) {
            boolean isDueNowOrPast = !item.getDueDate().isAfter(now);

            if (item.getPriority() == Priority.HIGH && isDueNowOrPast) {
                doFirst.add(item);
            } else if (item.getPriority() == Priority.HIGH) {
                schedule.add(item);
            } else if (isDueNowOrPast) {
                delegate.add(item);
            } else {
                eliminate.add(item);
            }
        }

        Comparator<MatrixItem> byDueDate = Comparator.comparing(MatrixItem::getDueDate);
        doFirst.sort(byDueDate);
        schedule.sort(byDueDate);
        delegate.sort(byDueDate);
        eliminate.sort(byDueDate);

        Map<String, List<MatrixItem>> matrix = new HashMap<>();
        matrix.put("doFirst", doFirst);
        matrix.put("schedule", schedule);
        matrix.put("delegate", delegate);
        matrix.put("eliminate", eliminate);

        return matrix;
    }


    public Map<Month, List<Task>> getTasksGroupedByMonth() {
        List<Task> allTasks = repo.findAll();
        return allTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .collect(Collectors.groupingBy(t -> t.getDueDate().getMonth()));
    }






    public void deleteTasksByYear(int year) {
        List<Task> tasksToDelete = repo.findAll().stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().getYear() == year)
                .toList();
        repo.deleteAll(tasksToDelete);
    }

    public void exportTasksForYearToExcel(int year, ServletOutputStream outputStream) throws IOException {
        List<Task> tasks = repo.findAll().stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().getYear() == year)
                .toList();

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Tasks " + year);

        HSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Title");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Priority");
        header.createCell(3).setCellValue("Due Date");
        header.createCell(4).setCellValue("Completed");

        int rowNum = 1;
        for (Task task : tasks) {
            HSSFRow row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(task.getTitle());
            row.createCell(1).setCellValue(task.getCategory());
            row.createCell(2).setCellValue(task.getPriority().toString());
            row.createCell(3).setCellValue(task.getDueDate().toString());
            row.createCell(4).setCellValue(task.isCompleted() ? "Yes" : "No");
        }

        workbook.write(outputStream);
        workbook.close();
    }
}
