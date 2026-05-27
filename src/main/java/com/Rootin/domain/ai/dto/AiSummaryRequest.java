package com.Rootin.domain.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiSummaryRequest(

        @NotNull(message = "tilId는 필수입니다.")
        Long tilId
) {}
