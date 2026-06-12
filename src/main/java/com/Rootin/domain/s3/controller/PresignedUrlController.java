package com.Rootin.domain.s3.controller;

import com.Rootin.domain.garden.service.PotService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtUserDetails;
import com.Rootin.domain.s3.dto.PresignedUrlRequest;
import com.Rootin.domain.s3.dto.PresignedUrlResponse;
import com.Rootin.global.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PresignedUrlController {

    private final S3Service s3Service;
    private final PotService potService;

    @PostMapping("/tils/image/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        Long userId = userDetails.getUserId();
        Long potId = request.getPotId();

        // DB에서 이 화분이 이 사용자의 것인지 검증하는 절차
        potService.validateOwnership(userId, potId);

        // TIL 본문의 이미지 저장Key
        String ext = extractExtension(request.getContentType());
        String objectKey = String.format("til-images/%d/%d/%s.%s",userId, potId, UUID.randomUUID(), ext);

        // S3에 접근하도록 임시 URL 제공 및 이미지 URL
        String presignedUrl = s3Service.generatePresignedPutUrl(objectKey, request.getContentType());
        String imageUrl = s3Service.getFileUrl(objectKey);

        return ResponseEntity.ok(ApiResponse.success(
                new PresignedUrlResponse(imageUrl, presignedUrl)
        ));
    }

    private String extractExtension(String contentType) {
        if (contentType == null) {
            throw CustomException.badRequest("contentType이 없습니다.");
        }

        return switch (contentType) {
            case "image/png"  -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw CustomException.badRequest("지원하지 않는 이미지 형식입니다: " + contentType);
        };
    }
}