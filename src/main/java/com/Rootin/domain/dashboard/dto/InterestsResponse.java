package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record InterestsResponse(
        List<MonthlyInterestDto> interests
) {}
