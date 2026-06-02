package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.service.GardenDashboardService;
import com.Rootin.domain.garden.service.PotService;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 화분 생성 및 조회를 제공하는 REST API 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/v1/pots")
@RequiredArgsConstructor
public class PotController {

    private final PotService potService;
    private final GardenDashboardService gardenDashboardService;

    /**
     * POST /api/v1/pots
     * 화분을 새로 생성합니다.
     */
    @PostMapping
    public ResponseEntity<PotResponse> createPot(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody PotCreateRequest request
    ) {
        PotResponse response = potService.createPot(userDetails.getUserId(), request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/pots
     * 로그인한 사용자의 모든 화분 목록을 요약 정보(PotSummaryResponse) DTO 목록으로 조회합니다.
     */
    @GetMapping
    public ResponseEntity<List<PotSummaryResponse>> getPots(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        List<PotSummaryResponse> response = potService.getPots(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/pots/{potId}
     * 특정 화분의 상세 정보를 조회합니다.
     * 사용자 권한(소유권)을 검증합니다.
     */
    @GetMapping("/{potId}")
    public ResponseEntity<PotResponse> getPot(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long potId
    ) {
        PotResponse response = potService.getPot(potId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/pots/{potId}/dashboard
     * 특정 화분의 상세 대시보드 화면(GD-03)에 띄울 정보를 복합 조회합니다.
     * 사용자 권한(소유권) 검증을 내부 서비스에서 진행합니다.
     */
    @GetMapping("/{potId}/dashboard")
    public ResponseEntity<GardenInfoResponse> getGardenDashboard(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long potId
    ) {
        GardenInfoResponse response = gardenDashboardService.getGardenDashboard(potId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
