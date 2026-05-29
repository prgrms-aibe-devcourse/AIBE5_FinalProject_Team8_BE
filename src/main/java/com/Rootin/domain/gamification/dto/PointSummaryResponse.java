package com.Rootin.domain.gamification.dto;

public record PointSummaryResponse(
        int currentPoint,
        int totalEarned,
        int totalUsed
) {}
