package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DraftSaveRequest(
        @NotNull Long potId,
        String title,
        String content,
        List<String> tags,
        // [S3 이미지 업로드 기능 추가] 임시저장 시 이미지 URL 목록 (없으면 null 또는 빈 리스트)
        List<String> imageUrls
) {}
