package com.Rootin.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TilTemplateCreateRequest(
        @NotBlank String title,
        @NotBlank String content
) {}
