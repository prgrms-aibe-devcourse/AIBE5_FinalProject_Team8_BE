package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.PresignedUrlResponse;
import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtUserDetails;
import com.Rootin.global.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final long MAX_PROFILE_IMAGE_SIZE = 1024 * 1024L; // 1MB

    private final UserService userService;
    private final S3Service s3Service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        UserMeResponse response = userService.getUserMe(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("유저 정보 조회 성공", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        userService.deleteUser(userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> updateMe(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        UserMeResponse response = userService.updateUserMe(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 성공", response));
    }

    /**
     * 프로필 이미지 업로드용 S3 Presigned URL 발급
     *
     * 처리 흐름:
     *   1. fileName의 확장자로 Content-Type 판단
     *   2. objectKey = profile-images/{userId}/{uuid}.{ext}
     *   3. S3Presigner로 PUT Presigned URL 생성 (유효 10분)
     *   4. presignedUrl + fileUrl 반환
     *
     * 프론트엔드는 presignedUrl로 S3에 직접 PUT 업로드 후,
     * fileUrl을 PATCH /users/me의 profileImageUrl에 담아 저장한다.
     *
     * @param fileName 업로드할 파일명 (확장자 포함, 예: profile.jpg)
     * @param fileSize 업로드할 파일 크기 (bytes) — 1MB(1,048,576) 초과 시 400 반환
     */
    @PostMapping("/me/profile-image/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getProfileImagePresignedUrl(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam String fileName,
            @RequestParam long fileSize
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }

        if (fileSize > MAX_PROFILE_IMAGE_SIZE) {
            throw CustomException.badRequest(
                    String.format("파일 크기는 %dMB 이하여야 합니다.", MAX_PROFILE_IMAGE_SIZE / (1024 * 1024))
            );
        }

        String ext = extractExtension(fileName);
        String contentType = resolveContentType(ext);
        String objectKey = String.format("profile-images/%d/%s.%s",
                userDetails.getUserId(), UUID.randomUUID(), ext);

        String presignedUrl = s3Service.generatePresignedPutUrl(objectKey, contentType);
        String fileUrl = s3Service.getFileUrl(objectKey);

        return ResponseEntity.ok(
                ApiResponse.success("Presigned URL 발급 성공", PresignedUrlResponse.of(presignedUrl, fileUrl))
        );
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw CustomException.badRequest("파일 확장자가 없습니다.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String resolveContentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "gif"         -> "image/gif";
            case "webp"        -> "image/webp";
            default -> throw CustomException.badRequest("지원하지 않는 이미지 형식입니다. (jpg, png, gif, webp 허용)");
        };
    }
}
