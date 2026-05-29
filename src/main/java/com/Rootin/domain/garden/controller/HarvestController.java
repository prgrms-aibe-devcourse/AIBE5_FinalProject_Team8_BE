package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.HarvestResponse;
import com.Rootin.domain.garden.service.HarvestService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pots")
@RequiredArgsConstructor
public class HarvestController {

    private final HarvestService harvestService;

    @PostMapping("/{potId}/harvest")
    public ResponseEntity<ApiResponse<HarvestResponse>> harvest(
            @AuthenticationPrincipal User user,
            @PathVariable Long potId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(harvestService.harvest(user.getId(), potId)));
    }
}
