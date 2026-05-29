package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record MonthlyInterestDto(
        String month,
        List<TagCountDto> topTags
) {}
