package com.Rootin.domain.garden.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HarvestedPlantResponse {
    private Long id;
    private Long plantId;
    private String name;
    private String imageUrl;
    private Boolean isDisplayed;
    private Integer positionX;
    private Integer positionY;
}
