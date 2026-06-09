package com.Rootin.domain.ai.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AiSummaryRequest(

        @NotNull(message = "potId는 필수입니다.")
        Long potId,

        // 특정 TIL만 선택해 요약할 때 사용. null/empty이면 potId 기반 전체 TIL 요약 (하위 호환)
        List<Long> tilIds
) {}
