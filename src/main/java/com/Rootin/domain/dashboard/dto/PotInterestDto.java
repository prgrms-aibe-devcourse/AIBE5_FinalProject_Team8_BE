package com.Rootin.domain.dashboard.dto;

import com.Rootin.domain.plant.entity.enums.GrowthStage;

import java.util.List;

public record PotInterestDto(
        Long potId,
        String title,
        long tilCount,
        int level,
        GrowthStage growthStage,
        List<String> topTags
) {}
