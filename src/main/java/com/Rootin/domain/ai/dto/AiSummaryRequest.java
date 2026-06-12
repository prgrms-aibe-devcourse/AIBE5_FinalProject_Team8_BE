package com.Rootin.domain.ai.dto;

import com.Rootin.domain.ai.constant.AiPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiSummaryRequest(

        @NotNull(message = "potId는 필수입니다.")
        Long potId,

        // 특정 TIL만 선택해 요약할 때 사용. null/empty이면 potId 기반 전체 TIL 요약 (하위 호환)
        @Size(max = AiPolicy.TIL_IDS_MAX_SIZE, message = "TIL은 최대 " + AiPolicy.TIL_IDS_MAX_SIZE + "개까지 선택할 수 있습니다.")
        List<Long> tilIds
) {}
