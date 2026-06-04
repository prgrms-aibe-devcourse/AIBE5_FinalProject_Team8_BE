package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotNull;

public record PotPlantRequest(
        @NotNull(message = "심기 방식은 필수입니다.")
        PlantingType type,

        Long sourcePlantItemId
) {
}
