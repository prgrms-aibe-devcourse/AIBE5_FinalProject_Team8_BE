// [S3 이미지 업로드 기능 추가] TilUpdateRequest
// imageUrls: 수정 시 새로 추가된 이미지 URL 목록 (Presigned URL로 S3에 미리 업로드된 것들)
// deletedImageIds: 기존 이미지 중 삭제할 이미지 레코드 ID 목록 → DB 삭제 + S3 파일 삭제
package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TilUpdateRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<String> tags,
        // 수정 시 새로 추가된 이미지 URL 목록 (없으면 null 또는 빈 리스트)
        List<String> imageUrls,
        // 삭제할 기존 이미지 ID 목록 (PostImageResponse.id 값, 없으면 null 또는 빈 리스트)
        List<Long> deletedImageIds
) {}
