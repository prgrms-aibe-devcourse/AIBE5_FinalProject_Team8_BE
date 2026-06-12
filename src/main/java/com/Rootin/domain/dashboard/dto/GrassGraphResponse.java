package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record GrassGraphResponse(
        List<GrassCell> cells,
        int currentStreak,
        int maxStreak
) {}
