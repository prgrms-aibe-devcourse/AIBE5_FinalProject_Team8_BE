package com.Rootin.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

// Presigned URL 응답 DTO
// API: [POST] /api/v1/users/me/profile-image/presigned-url
@Getter
@Builder
public class PresignedUrlResponse {

    /** S3에 직접 PUT 요청할 URL (유효시간 10분) */
    private String presignedUrl;

    /** 업로드 완료 후 PATCH /users/me에 전달할 S3 파일 URL */
    private String fileUrl;

    public static PresignedUrlResponse of(String presignedUrl, String fileUrl) {
        return PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .fileUrl(fileUrl)
                .build();
    }
}
