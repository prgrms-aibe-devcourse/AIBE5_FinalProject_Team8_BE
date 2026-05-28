package com.Rootin.domain.dashboard.dto;

/**
 * 사용자 전체 학습 통계 응답 DTO.
 * totalLearningDays: TIL을 작성한 누적 날짜 수 (중복 날짜 제외)
 * maxStreak: 전체 기간 기준 최대 연속 작성일 수
 */
public record PersonalStatsResponse(
        long totalTilCount,
        int totalContentLength,
        int totalLearningDays,
        int totalExpGained,
        int totalPointEarned,
        int currentStreak,
        int maxStreak,
        int currentPoints
) {}
