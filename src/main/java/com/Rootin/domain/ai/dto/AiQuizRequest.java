package com.Rootin.domain.ai.dto;

import com.Rootin.domain.ai.constant.AiPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AiQuizRequest(

        @NotNull(message = "potId는 필수입니다.")
        Long potId,

        @NotNull(message = "count는 필수입니다.")
        @Min(value = 1, message = "문항 수는 최소 1개입니다.")
        @Max(value = AiPolicy.QUIZ_MAX_COUNT, message = "문항 수는 최대 " + AiPolicy.QUIZ_MAX_COUNT + "개입니다.")
        Integer count,

        // 특정 TIL만 선택해 퀴즈를 생성할 때 사용. null/empty이면 potId 기반 전체 TIL 사용 (하위 호환)
        List<Long> tilIds
) {}
