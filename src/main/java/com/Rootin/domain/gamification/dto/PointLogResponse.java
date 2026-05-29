package com.Rootin.domain.gamification.dto;

import com.Rootin.domain.gamification.entity.PointLog;

import java.time.LocalDateTime;

public record PointLogResponse(
        String type,
        int amount,
        String reason,
        LocalDateTime createdAt
) {
    public static PointLogResponse from(PointLog log) {
        return new PointLogResponse(
                log.getAmount() > 0 ? "EARN" : "USE",
                Math.abs(log.getAmount()),
                log.getReason().getDescription(),
                log.getCreatedAt()
        );
    }
}
