package com.Rootin.domain.gamification.controller;

import com.Rootin.domain.gamification.dto.PointLogResponse;
import com.Rootin.domain.gamification.dto.PointSummaryResponse;
import com.Rootin.domain.gamification.service.PointService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PointSummaryResponse>> getSummary(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointService.getPointSummary(user.getId())));
    }

    @GetMapping("/me/history")
    public ResponseEntity<ApiResponse<Page<PointLogResponse>>> getHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                pointService.getPointHistory(user.getId(), PageRequest.of(page, size))
        ));
    }
}
