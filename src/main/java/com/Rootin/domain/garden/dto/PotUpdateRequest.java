package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PotUpdateRequest {

    @NotBlank(message = "화분 제목은 필수 입력 항목입니다.")
    @Size(max = PotRequestValidation.TITLE_MAX_LENGTH, message = "화분 제목은 최대 {max}자까지 입력 가능합니다.")
    private String title;

    @Size(max = PotRequestValidation.DESCRIPTION_MAX_LENGTH, message = "화분 소개글은 최대 {max}자까지 입력 가능합니다.")
    private String description;
}
