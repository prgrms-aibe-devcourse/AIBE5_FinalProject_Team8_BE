package com.Rootin.domain.garden.dto;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PotGardenResponse {
    private Long id;
    private String title;
    private Integer level;
    private String plantName;
    private GrowthStage growthStage;
    private String imageUrl;
    private Boolean isDisplayed;
    private Integer positionX;
    private Integer positionY;
}
