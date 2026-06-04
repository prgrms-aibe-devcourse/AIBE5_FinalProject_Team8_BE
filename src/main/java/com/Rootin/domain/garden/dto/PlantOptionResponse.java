package com.Rootin.domain.garden.dto;

import java.time.LocalDateTime;

public record PlantOptionResponse(
        Long sourcePlantItemId,
        Long plantId,
        String plantName,
        String rarity,
        String imageUrl,
        Integer harvestedLevel,
        LocalDateTime harvestedAt
) {
}
