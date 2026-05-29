package com.Rootin.domain.dashboard.controller;

import com.Rootin.domain.dashboard.dto.*;
import com.Rootin.domain.dashboard.service.DashboardService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/grass")
    public ResponseEntity<ApiResponse<GrassGraphResponse>> getGrassGraph(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getGrassGraph(user.getId(), targetYear)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getWeeklyStats(user.getId())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PersonalStatsResponse>> getPersonalStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPersonalStats(user.getId())));
    }

    @GetMapping("/distribution")
    public ResponseEntity<ApiResponse<DistributionResponse>> getDistribution(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDistribution(user.getId())));
    }

    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<InterestsResponse>> getInterests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getInterests(user.getId(), months)));
    }

    @GetMapping("/quests")
    public ResponseEntity<ApiResponse<QuestResponse>> getQuests(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getQuests(user.getId())));
    }
}
