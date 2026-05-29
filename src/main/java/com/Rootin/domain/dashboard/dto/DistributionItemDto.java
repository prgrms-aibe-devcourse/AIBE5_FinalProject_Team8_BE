package com.Rootin.domain.dashboard.dto;

public record DistributionItemDto(
        Long potId,
        String potName,
        long tilCount,
        double ratio
) {}
