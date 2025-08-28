package com.yourapp.dashboard.productivity_dashboard.scheduler;

import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.model.Task;
import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import com.yourapp.dashboard.productivity_dashboard.repository.TaskRepository;
import com.yourapp.dashboard.productivity_dashboard.service.Priority;
import com.yourapp.dashboard.productivity_dashboard.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final HabitLogRepository habitLogRepository;
    private final TelegramService telegramService;

    private static final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    // every 15 minutes
    @Scheduled(cron = "0 */15 * * * *")
    public void sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(1);

        List<Task> tasks = taskRepository
                .findByCompletedFalseAndNotifSentFalseAndDueDateBetween(now, windowEnd)
                .stream()
                .filter(t -> t.getPriority() == Priority.HIGH)
                .collect(Collectors.toList());

        List<HabitLog> habits = habitLogRepository
                .findByCompletedFalseAndNotifSentFalseAndScheduledDateTimeBetween(now, windowEnd);

        if (tasks.isEmpty() && habits.isEmpty()) return;

        StringBuilder sb = new StringBuilder("📅 Upcoming items (next hour)\n\n");

        if (!tasks.isEmpty()) {
            sb.append("🔴 Tasks:\n");
            tasks.forEach(t -> sb.append(" • ")
                    .append(t.getTitle())
                    .append(" at ")
                    .append(t.getDueDate().format(timeFmt))
                    .append("\n"));
            sb.append("\n");
        }

        if (!habits.isEmpty()) {
            sb.append("🔵 Habits:\n");
            habits.forEach(h -> sb.append(" • ")
                    .append(h.getHabit().getName())
                    .append(" at ")
                    .append(h.getScheduledDateTime().format(timeFmt))
                    .append("\n"));
        }

        telegramService.sendMessage(sb.toString());

        // mark sent
        tasks.forEach(t -> { t.setNotifSent(true); taskRepository.save(t); });
        habits.forEach(h -> { h.setNotifSent(true); habitLogRepository.save(h); });
    }
}
