package com.Rootin.domain.til.controller;

import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.request.TilUpdateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.service.TilService;
import com.Rootin.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    @PostMapping
    public ResponseEntity<ApiResponse<TilResponse>> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TilCreateRequest request
    ) {
        TilResponse response = tilService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{tilId}")
    public ResponseEntity<ApiResponse<TilResponse>> findById(
            @PathVariable Long tilId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tilService.findById(tilId, userId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<TilResponse>>> findMyTils(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Long potId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tilService.findMyTils(userId, potId, page, size, sort)));
    }

    @PutMapping("/{tilId}")
    public ResponseEntity<ApiResponse<TilResponse>> update(
            @PathVariable Long tilId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TilUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tilService.update(tilId, userId, request)));
    }

    @DeleteMapping("/{tilId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long tilId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        tilService.delete(tilId, userId);
        return ResponseEntity.noContent().build();
    }
}
