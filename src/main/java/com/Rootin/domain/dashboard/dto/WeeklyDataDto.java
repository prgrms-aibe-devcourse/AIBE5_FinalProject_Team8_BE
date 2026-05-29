package com.Rootin.domain.dashboard.dto;

import java.time.LocalDate;

public record WeeklyDataDto(
        LocalDate date,
        int tilCount,
        int charCount
) {}
