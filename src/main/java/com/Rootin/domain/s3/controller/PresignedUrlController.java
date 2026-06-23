// [S3 이미지 업로드 기능 추가] PresignedUrlController
// S3 경로 형식을 요구사항에 맞게 변경: til-images/{UUID}/{potId}/{tilId}/{filename}
//   - {UUID}: 랜덤 UUID (이미지 디렉터리 고유성 보장)
//   - {potId}: 화분 ID
//   - {tilId}: TIL ID (신규 TIL 작성 시 미전달이면 0 사용)
//   - {filename}: UUID 기반 파일명 + 확장자
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

    /**
     * TIL 본문 이미지 업로드용 Presigned PUT URL 발급
     *
     * S3 저장 경로: til-images/{UUID}/{potId}/{tilId}/{filename.ext}
     *   - {UUID}  : 디렉터리 고유성을 위한 랜덤 UUID
     *   - {potId} : 화분 ID (요청 필수)
     *   - {tilId} : TIL ID (수정 시 전달, 신규 작성 시 미전달→0 사용)
     *   - {filename}: UUID 기반 파일명 + 확장자
     *
     * 클라이언트 사용 흐름:
     *   1. 이 API로 presignedUrl + imageUrl 수신
     *   2. presignedUrl로 S3에 이미지 직접 업로드 (PUT)
     *   3. imageUrl을 TIL 본문에 삽입
     *   4. TIL 발행 시 imageUrls 목록에 imageUrl 포함하여 POST/PUT 요청
     */
    @PostMapping("/tils/image/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        Long userId = userDetails.getUserId();
        Long potId = request.getPotId();

        // DB에서 이 화분이 이 사용자의 것인지 검증하는 절차
        potService.validateOwnership(userId, potId);

        // [S3 이미지 업로드 기능 추가] 요구사항 경로 형식 적용: til-images/{UUID}/{potId}/{tilId}/{filename}
        // tilId가 미전달(null)이면 신규 TIL 작성으로 간주하여 0을 사용한다.
        String ext = extractExtension(request.getContentType());
        long tilId = (request.getTilId() != null) ? request.getTilId() : 0L;
        String directoryUuid = UUID.randomUUID().toString();
        String fileUuid = UUID.randomUUID().toString();
        String objectKey = String.format("til-images/%s/%d/%d/%s.%s",
                directoryUuid, potId, tilId, fileUuid, ext);

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