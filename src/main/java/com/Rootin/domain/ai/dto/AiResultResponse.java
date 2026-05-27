package com.Rootin.domain.ai.dto;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.ToolType;

import java.time.LocalDateTime;

public record AiResultResponse(
        Long resultId,
        ToolType type,
        String content,
        Long potId,
        LocalDateTime createdAt
) {
    /**
     * potId는 ai_results 테이블에 저장되지 않으므로 서비스 레이어에서 계산 후 전달
     */
    public static AiResultResponse of(AiResult aiResult, Long potId) {
        return new AiResultResponse(
                aiResult.getId(),
                aiResult.getToolType(),
                aiResult.getResultContent(),
                potId,
                aiResult.getCreatedAt()
        );
    }
}
