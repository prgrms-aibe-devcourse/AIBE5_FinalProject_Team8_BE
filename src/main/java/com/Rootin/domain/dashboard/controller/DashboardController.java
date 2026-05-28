package com.Rootin.domain.dashboard.controller;

import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
import com.Rootin.domain.dashboard.dto.InterestDistributionResponse;
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

    /**
     * GET /api/v1/dashboard/grass?year=2025
     * 잔디 그래프 데이터를 조회합니다.
     * year 미입력 시 현재 연도를 기준으로 조회합니다.
     * FIXME: JWT 도입 후 X-USER-ID 헤더를 @AuthenticationPrincipal로 교체
     */
    @GetMapping("/grass")
    public ResponseEntity<ApiResponse<GrassGraphResponse>> getGrassGraph(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getGrassGraph(userId, targetYear)));
    }

    /**
     * GET /api/v1/dashboard/weekly
     * 이번 주(월~일) TIL 작성 현황을 조회합니다.
     * FIXME: JWT 도입 후 X-USER-ID 헤더를 @AuthenticationPrincipal로 교체
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getWeeklyStats(userId)));
    }

    /**
     * GET /api/v1/dashboard/stats
     * 사용자 전체 학습 통계를 조회합니다.
     * FIXME: JWT 도입 후 X-USER-ID 헤더를 @AuthenticationPrincipal로 교체
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PersonalStatsResponse>> getPersonalStats(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPersonalStats(userId)));
    }

    /**
     * GET /api/v1/dashboard/interests
     * 화분별 TIL 수, 레벨, 성장 단계, 상위 태그 분포를 조회합니다.
     * FIXME: JWT 도입 후 X-USER-ID 헤더를 @AuthenticationPrincipal로 교체
     */
    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<InterestDistributionResponse>> getInterestDistribution(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getInterestDistribution(userId)));
    }
}
