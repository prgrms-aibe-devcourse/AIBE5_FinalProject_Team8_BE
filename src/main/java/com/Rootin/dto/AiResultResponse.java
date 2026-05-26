package com.Rootin.dto;

import com.Rootin.domain.AiResult;
import com.Rootin.domain.enums.ToolType;

import java.time.LocalDateTime;

public record AiResultResponse(
        Long resultId,
        ToolType type,
        String content,
        Long tilId,
        LocalDateTime createdAt
) {
    public static AiResultResponse from(AiResult aiResult) {
        return new AiResultResponse(
                aiResult.getId(),
                aiResult.getToolType(),
                aiResult.getResultContent(),
                aiResult.getPost().getId(),
                aiResult.getCreatedAt()
        );
    }
}
