package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.dto.HabitLogDto;
import com.yourapp.dashboard.productivity_dashboard.model.HabitLog;
import com.yourapp.dashboard.productivity_dashboard.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitApiController {

    private final HabitService habitService;

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<HabitLogDto>> getLogs(@PathVariable Long id,
                                                     @RequestParam int page,
                                                     @RequestParam int size) {
        return habitService.getHabit(id)
                .map(habit -> {
                    List<HabitLogDto> dto = habitService.getLogsPage(habit, page, size)
                            .map(l -> new HabitLogDto(l.getId(), l.getScheduledDateTime(), l.isCompleted()))
                            .getContent();
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/logs/{logId}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long logId){
        habitService.markDone(logId);
        return ResponseEntity.ok().build();
    }
}
