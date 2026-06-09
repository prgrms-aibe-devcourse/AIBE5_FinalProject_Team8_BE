package com.Rootin.domain.til.controller;

import com.Rootin.domain.til.dto.request.DraftSaveRequest;
import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.request.TilUpdateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.service.TilService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    @PostMapping
    public ResponseEntity<ApiResponse<TilResponse>> create(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody TilCreateRequest request
    ) {
        TilResponse response = tilService.create(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{tilId}")
    public ResponseEntity<ApiResponse<TilResponse>> findById(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.findById(tilId, userDetails.getUserId())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<TilResponse>>> findMyTils(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(required = false) Long potId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tilService.findMyTils(userDetails.getUserId(), potId, page, size, sort, keyword, tag)));
    }

    @PutMapping("/{tilId}")
    public ResponseEntity<ApiResponse<TilResponse>> update(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody TilUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.update(tilId, userDetails.getUserId(), request)));
    }

    @DeleteMapping("/{tilId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long tilId,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        tilService.delete(tilId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<TilResponse>> saveDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody DraftSaveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.saveDraft(userDetails.getUserId(), request)));
    }

    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<TilResponse>> getDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam Long potId
    ) {
        return ResponseEntity.ok(ApiResponse.success(tilService.getDraft(userDetails.getUserId(), potId)));
    }

    @DeleteMapping("/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam Long potId
    ) {
        tilService.deleteDraft(userDetails.getUserId(), potId);
        return ResponseEntity.noContent().build();
    }
}
