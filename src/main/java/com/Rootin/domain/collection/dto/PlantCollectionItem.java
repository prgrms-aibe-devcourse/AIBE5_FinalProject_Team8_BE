package com.Rootin.domain.collection.dto;

import java.time.LocalDateTime;

public record PlantCollectionItem(
        String plantType,
        String rarity,
        boolean isCollected,
        LocalDateTime collectedAt,
        String imageUrl,
        String currentPotName,
        Integer harvestedLevel
) {}
