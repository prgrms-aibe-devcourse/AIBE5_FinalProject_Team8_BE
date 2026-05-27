package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TilUpdateRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<String> tags
) {}
