package com.Rootin.domain.dashboard.dto;

import java.util.List;

/**
 * 잔디 그래프 API 응답 DTO.
 * cells: 해당 연도의 날짜별 작성 현황
 * currentStreak: 현재 연속 작성일 수
 * maxStreak: 조회 연도 내 최대 연속 작성일 수
 */
public record GrassGraphResponse(
        int year,
        List<GrassCell> cells,
        int currentStreak,
        int maxStreak
) {}
