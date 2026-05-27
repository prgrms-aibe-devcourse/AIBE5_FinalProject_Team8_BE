package com.Rootin.domain.til.controller;

import com.Rootin.domain.til.dto.request.TilTemplateCreateRequest;
import com.Rootin.domain.til.dto.response.TilTemplateResponse;
import com.Rootin.domain.til.service.TilTemplateService;
import com.Rootin.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/til-templates")
@RequiredArgsConstructor
public class TilTemplateController {

    private final TilTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TilTemplateResponse>>> getTemplates(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplates(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TilTemplateResponse>> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TilTemplateCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(templateService.create(userId, request)));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long templateId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        templateService.delete(userId, templateId);
        return ResponseEntity.noContent().build();
    }
}
