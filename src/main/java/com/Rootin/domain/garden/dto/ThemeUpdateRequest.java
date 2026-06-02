package com.Rootin.domain.garden.dto;

import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ThemeUpdateRequest {

    @NotNull(message = "변경할 테마 값이 필요합니다.")
    private GardenTheme theme;
}
