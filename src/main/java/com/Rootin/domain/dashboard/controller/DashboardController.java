package com.Rootin.domain.dashboard.controller;

import com.Rootin.domain.dashboard.dto.*;
import com.Rootin.domain.dashboard.service.DashboardService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.jwt.JwtUserDetails;
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
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getGrassGraph(userDetails.getUserId(), targetYear)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getWeeklyStats(userDetails.getUserId())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PersonalStatsResponse>> getPersonalStats(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getPersonalStats(userDetails.getUserId())));
    }

    @GetMapping("/distribution")
    public ResponseEntity<ApiResponse<DistributionResponse>> getDistribution(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDistribution(userDetails.getUserId())));
    }

    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<InterestsResponse>> getInterests(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getInterests(userDetails.getUserId(), months)));
    }

    @GetMapping("/quests")
    public ResponseEntity<ApiResponse<QuestResponse>> getQuests(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getQuests(userDetails.getUserId())));
    }
}
