package com.Rootin.domain.collection.controller;

import com.Rootin.domain.collection.dto.PlantCollectionResponse;
import com.Rootin.domain.collection.service.CollectionService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<PlantCollectionResponse>> getPlants(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getPlants(user.getId())));
    }
}
