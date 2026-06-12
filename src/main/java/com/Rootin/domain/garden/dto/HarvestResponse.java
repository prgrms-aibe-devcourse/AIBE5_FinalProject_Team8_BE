package com.Rootin.domain.garden.dto;

public record HarvestResponse(
        String harvestedPlantName,
        String harvestedRarity,
        int harvestedLevel,
        int harvestedStageIndex,
        String nextPlantName,
        String nextRarity
) {}
