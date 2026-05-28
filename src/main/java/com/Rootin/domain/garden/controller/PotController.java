package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.service.GardenDashboardService;
import com.Rootin.domain.garden.service.PotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 화분 생성 및 조회를 제공하는 REST API 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/pots")
@RequiredArgsConstructor
public class PotController {

    private final PotService potService;
    private final GardenDashboardService gardenDashboardService;

    /**
     * POST /api/pots
     * 화분을 새로 생성합니다.
     *
     * FIXME [보안 경고]: 현재 JWT 로그인 연동 전 임시 테스트용으로 헤더에서 X-USER-ID를 직접 받아
     * 사용자를 인증하고 있습니다. 이 방식은 악의적인 클라이언트가 임의의 유저 ID 헤더를 설정하여
     * 타인의 화분을 생성하거나 조회하는 심각한 보안 취약점(ID Spoofing)을 발생시킬 수 있습니다.
     * 향후 JWT 토큰 인증 아키텍처가 구축되는 즉시, 헤더 직접 조회가 아닌
     * SecurityContextHolder의 AuthenticationPrincipal 객체에서 로그인된 실제 유저 ID 정보를
     * 안전하게 주입받도록 리팩토링해야 합니다.
     */
    @PostMapping
    public ResponseEntity<PotResponse> createPot(
            @RequestHeader("X-USER-ID") Long userId, // FIXME: JWT 도입 후 제거 예정인 임시 ID 헤더
            @Valid @RequestBody PotCreateRequest request
    ) {
        PotResponse response = potService.createPot(userId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/pots
     * 로그인한 사용자의 모든 화분 목록을 요약 정보(PotSummaryResponse) DTO 목록으로 조회합니다.
     *
     * FIXME [보안 경고]: JWT 토큰 도입 시 Spring Security 인증 정보(@AuthenticationPrincipal)로 교체 필수.
     */
    @GetMapping
    public ResponseEntity<List<PotSummaryResponse>> getPots(
            @RequestHeader("X-USER-ID") Long userId // FIXME: JWT 도입 후 제거 예정인 임시 ID 헤더
    ) {
        List<PotSummaryResponse> response = potService.getPots(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/pots/{potId}
     * 특정 화분의 상세 정보를 조회합니다.
     * 사용자 권한(소유권)을 검증합니다.
     * FIXME [보안 경고]: JWT 토큰 도입 시 Spring Security 인증 정보(@AuthenticationPrincipal)로 교체 필수.
     */
    @GetMapping("/{potId}")
    public ResponseEntity<PotResponse> getPot(
            @RequestHeader("X-USER-ID") Long userId, // FIXME: JWT 도입 후 제거 예정인 임시 ID 헤더
            @PathVariable Long potId
    ) {
        PotResponse response = potService.getPot(potId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/pots/{potId}/dashboard
     * 특정 화분의 상세 대시보드 화면(GD-03)에 띄울 정보를 복합 조회합니다.
     * 사용자 권한(소유권) 검증을 내부 서비스에서 진행합니다.
     * FIXME [보안 경고]: JWT 토큰 도입 시 Spring Security 인증 정보(@AuthenticationPrincipal)로 교체 필수.
     */
    @GetMapping("/{potId}/dashboard")
    public ResponseEntity<GardenInfoResponse> getGardenDashboard(
            @RequestHeader("X-USER-ID") Long userId, // FIXME: JWT 도입 후 제거 예정인 임시 ID 헤더
            @PathVariable Long potId
    ) {
        GardenInfoResponse response = gardenDashboardService.getGardenDashboard(potId, userId);
        return ResponseEntity.ok(response);
    }
}
