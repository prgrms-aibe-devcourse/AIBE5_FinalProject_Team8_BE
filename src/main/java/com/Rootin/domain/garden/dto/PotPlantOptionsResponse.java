package com.Rootin.domain.garden.dto;

import java.util.List;

public record PotPlantOptionsResponse(
        Long potId,
        boolean canPlant,
        String unavailableReason,
        boolean randomSeedAvailable,
        PotPlantResponse currentPlant,
        List<PlantOptionResponse> harvestedPlants
) {
}
