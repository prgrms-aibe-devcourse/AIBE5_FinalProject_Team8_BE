package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record DistributionResponse(
        List<DistributionItemDto> distribution
) {}
