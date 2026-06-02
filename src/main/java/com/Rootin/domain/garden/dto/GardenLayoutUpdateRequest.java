package com.Rootin.domain.garden.dto;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GardenLayoutUpdateRequest {

    @Valid
    private List<LayoutUpdateDto> pots = new ArrayList<>();

    @Valid
    private List<LayoutUpdateDto> harvestedPlants = new ArrayList<>();
}
