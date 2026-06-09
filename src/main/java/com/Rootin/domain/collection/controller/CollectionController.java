package com.Rootin.domain.collection.controller;

import com.Rootin.domain.collection.dto.CollectionDexResponse;
import com.Rootin.domain.collection.service.CollectionService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.jwt.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping("/plants")
    public ResponseEntity<ApiResponse<CollectionDexResponse>> getPlants(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getPlants(userDetails.getUserId())));
    }
}
