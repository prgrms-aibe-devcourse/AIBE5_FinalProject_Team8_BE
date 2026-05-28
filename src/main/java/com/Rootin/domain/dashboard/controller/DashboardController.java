package com.Rootin.domain.dashboard.controller;

import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
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
}
