package com.Rootin.domain.til.controller;

import com.Rootin.domain.til.dto.request.TilTemplateCreateRequest;
import com.Rootin.domain.til.dto.response.TilTemplateResponse;
import com.Rootin.domain.til.service.TilTemplateService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/til-templates")
@RequiredArgsConstructor
public class TilTemplateController {

    private final TilTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TilTemplateResponse>>> getTemplates(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplates(userDetails.getUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TilTemplateResponse>> create(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody TilTemplateCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(templateService.create(userDetails.getUserId(), request)));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long templateId,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        templateService.delete(userDetails.getUserId(), templateId);
        return ResponseEntity.noContent().build();
    }
}
