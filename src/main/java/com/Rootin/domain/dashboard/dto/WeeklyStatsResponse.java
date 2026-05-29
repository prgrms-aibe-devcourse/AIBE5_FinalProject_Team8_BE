package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record WeeklyStatsResponse(
        List<WeeklyDataDto> weeklyData,
        int thisWeekTotal,
        int lastWeekTotal
) {}
