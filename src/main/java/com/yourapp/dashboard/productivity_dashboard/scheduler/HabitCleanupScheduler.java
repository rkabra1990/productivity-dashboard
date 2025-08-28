package com.yourapp.dashboard.productivity_dashboard.scheduler;

import com.yourapp.dashboard.productivity_dashboard.repository.HabitLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HabitCleanupScheduler {

    private final HabitLogRepository logRepository;

    // daily at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        logRepository.deleteByScheduledDateTimeBefore(cutoff);
    }
}
