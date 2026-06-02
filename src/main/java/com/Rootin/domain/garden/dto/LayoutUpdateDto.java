package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LayoutUpdateDto {

    @NotNull(message = "아이템 ID는 필수입니다.")
    private Long id;

    @NotNull(message = "정원 배치 여부(isDisplayed)는 필수입니다.")
    private Boolean isDisplayed;

    private Integer positionX;
    private Integer positionY;
}
