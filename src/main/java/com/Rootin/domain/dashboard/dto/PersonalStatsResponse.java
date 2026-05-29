package com.Rootin.domain.dashboard.dto;

public record PersonalStatsResponse(
        long totalTilCount,
        int totalCharCount,
        int totalStudyDays,
        int currentStreak,
        int longestStreak,
        int currentPoints
) {}
