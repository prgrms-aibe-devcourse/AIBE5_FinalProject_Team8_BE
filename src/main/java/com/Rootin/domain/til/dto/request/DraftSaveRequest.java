package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DraftSaveRequest(
        @NotNull Long potId,
        String title,
        String content,
        List<String> tags
) {}
