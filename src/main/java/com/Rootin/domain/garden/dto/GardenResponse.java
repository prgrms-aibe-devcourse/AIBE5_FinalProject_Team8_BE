package com.Rootin.domain.garden.dto;

import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GardenResponse {
    private GardenTheme theme;
    private List<PotGardenResponse> pots;
    private List<HarvestedPlantResponse> harvestedPlants;
}
