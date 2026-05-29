package com.Rootin.domain.gamification.dto;

import com.Rootin.domain.gamification.entity.PointLog;

import java.time.LocalDateTime;

public record PointLogResponse(
        Long id,
        String reason,
        int amount,
        LocalDateTime createdAt
) {
    public static PointLogResponse from(PointLog log) {
        return new PointLogResponse(
                log.getId(),
                log.getReason().getDescription(),
                log.getAmount(),
                log.getCreatedAt()
        );
    }
}
