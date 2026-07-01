// [S3 이미지 업로드 기능 추가] TilCreateRequest
// imageUrls 필드 추가: 클라이언트가 Presigned URL로 S3에 미리 업로드한 이미지 URL 목록.
// 순서가 imageOrder로 DB에 저장되므로 리스트 순서 유지가 중요하다.
package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TilCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull Long potId,
        List<String> tags,
        // S3에 이미 업로드된 이미지 URL 목록 (삽입 순서대로 전달, 없으면 null 또는 빈 리스트)
        List<String> imageUrls
) {}
