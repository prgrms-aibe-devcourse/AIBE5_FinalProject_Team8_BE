package com.Rootin.domain.dashboard.controller;

import com.Rootin.domain.dashboard.dto.DistributionResponse;
import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
import com.Rootin.domain.dashboard.dto.InterestsResponse;
import com.Rootin.domain.dashboard.dto.PersonalStatsResponse;
import com.Rootin.domain.dashboard.dto.WeeklyStatsResponse;
import com.Rootin.domain.dashboard.service.DashboardService;
import com.Rootin.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // FIXME: JWT 도입 후 X-USER-ID 헤더를 @AuthenticationPrincipal로 교체

    @GetMapping("/grass")
    public ResponseEntity<ApiResponse<GrassGraphResponse>> getGrassGraph(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getGrassGraph(userId, targetYear)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getWeeklyStats(userId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PersonalStatsResponse>> getPersonalStats(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPersonalStats(userId)));
    }

    @GetMapping("/distribution")
    public ResponseEntity<ApiResponse<DistributionResponse>> getDistribution(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDistribution(userId)));
    }

    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<InterestsResponse>> getInterests(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getInterests(userId, months)));
    }
}
