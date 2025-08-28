package com.yourapp.dashboard.productivity_dashboard.dto;

import java.time.LocalDateTime;

public record HabitLogDto(Long id, LocalDateTime scheduledDateTime, boolean completed) {}
