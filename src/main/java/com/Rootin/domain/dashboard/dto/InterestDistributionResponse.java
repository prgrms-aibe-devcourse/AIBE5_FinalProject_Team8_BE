package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record InterestDistributionResponse(
        List<PotInterestDto> pots
) {}
