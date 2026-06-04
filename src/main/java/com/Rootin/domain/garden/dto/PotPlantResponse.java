package com.Rootin.domain.garden.dto;

import com.Rootin.domain.plant.entity.enums.GrowthStage;

public record PotPlantResponse(
        Long potId,
        Long plantItemId,
        Long plantId,
        String plantName,
        String rarity,
        GrowthStage growthStage,
        Integer growthExp
) {
}
