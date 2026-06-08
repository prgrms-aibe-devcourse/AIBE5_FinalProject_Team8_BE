package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PotUpdateRequest {

    private static final int TITLE_MAX_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 25;

    @NotBlank(message = "화분 제목은 필수 입력 항목입니다.")
    @Size(max = TITLE_MAX_LENGTH, message = "화분 제목은 최대 10자까지 입력 가능합니다.")
    private String title;

    @Size(max = DESCRIPTION_MAX_LENGTH, message = "화분 소개글은 최대 25자까지 입력 가능합니다.")
    private String description;
}
