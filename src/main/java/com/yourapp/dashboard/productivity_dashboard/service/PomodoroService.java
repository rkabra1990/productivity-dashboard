package com.yourapp.dashboard.productivity_dashboard.service;

import com.yourapp.dashboard.productivity_dashboard.model.Habit;
import com.yourapp.dashboard.productivity_dashboard.model.PomodoroSession;
import com.yourapp.dashboard.productivity_dashboard.model.Task;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.PomodoroRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class PomodoroService {

    @Autowired
    private PomodoroRepository pomodoroRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitRepository habitRepository;
    // Save a new Pomodoro session
    public void save(PomodoroSession session) {
        if (session.getStartTime() == null) {
            session.setStartTime(LocalDateTime.now());
        }
        pomodoroRepository.save(session);
    }

    // Get all sessions
    public List<PomodoroSession> findAll() {
        return pomodoroRepository.findAll();
    }

    // Get sessions by date
    public List<PomodoroSession> getTodayPomodoroSessions() {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return pomodoroRepository.findAll()
                .stream()
                .filter(s -> !s.getStartTime().isBefore(startOfDay) && !s.getStartTime().isAfter(endOfDay))
                .toList();
    }
    public int getTodayPomodoroMinutes() {
        return getTodayPomodoroSessions()
                .stream()
                .mapToInt(s -> s.getSessions() * 25)
                .sum();
    }

    // Get total sessions completed (optional summary)
    public int totalSessions() {
        return pomodoroRepository.findAll().stream()
                .mapToInt(PomodoroSession::getSessions)
                .sum();
    }

    public long[] sumPomodoroMinutesByDay(LocalDateTime start, LocalDateTime end) {
        long[] weeklyPomodoro = new long[7];
        pomodoroRepository.sumPomodoroMinutesByDay(start, end)
                .forEach(row -> {
                    int dayOfWeek = (int) row[0]; // 1 = Sunday
                    Long totalMinutes = (Long) row[1];
                    weeklyPomodoro[dayOfWeek - 1] = (totalMinutes != null) ? totalMinutes : 0L;
                });
        return weeklyPomodoro;
    }

    public Optional<Task> findByTaskId(Long taskId) {
        return taskRepository.findById(taskId);
    }

    public Optional<Habit> findByHabitId(Long habitId) {
        return habitRepository.findById(habitId);
    }
}
