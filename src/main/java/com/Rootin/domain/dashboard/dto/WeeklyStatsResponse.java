package com.Rootin.domain.dashboard.dto;

/**
 * 이번 주(월~일) TIL 작성 현황 응답 DTO.
 */
public record WeeklyStatsResponse(
        int weeklyTilCount,
        int weeklyExpGained,
        int weeklyPointGained,
        int weeklyContentLength
) {}
