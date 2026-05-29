package com.Rootin.domain.garden.dto;

public record HarvestResponse(
        String harvestedPlantName,
        String harvestedRarity,
        int harvestedLevel,
        String nextPlantName,
        String nextRarity
) {}
