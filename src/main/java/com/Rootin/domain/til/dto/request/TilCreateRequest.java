package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TilCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull Long potId,
        List<String> tags
) {}
