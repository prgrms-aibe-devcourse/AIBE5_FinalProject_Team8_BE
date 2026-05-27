package com.Rootin.domain.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiSummaryRequest(

        @NotNull(message = "potId는 필수입니다.")
        Long potId
) {}
