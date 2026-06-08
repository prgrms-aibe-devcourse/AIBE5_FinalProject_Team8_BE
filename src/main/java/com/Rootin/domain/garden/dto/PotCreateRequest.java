package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.Rootin.domain.garden.constant.PotPolicy.DESCRIPTION_MAX_LENGTH;
import static com.Rootin.domain.garden.constant.PotPolicy.TITLE_MAX_LENGTH;

/**
 * 화분 생성 요청 DTO입니다.
 * 화면 레이아웃 안정화를 위해 API 요청 단계에서 제목은 10자, 소개글은 25자까지 허용합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PotCreateRequest {

    @NotBlank(message = "화분 제목은 필수 입력 항목입니다.")
    @Size(max = TITLE_MAX_LENGTH, message = "화분 제목은 최대 {max}자까지 입력 가능합니다.")
    private String title;

    @Size(max = DESCRIPTION_MAX_LENGTH, message = "화분 소개글은 최대 {max}자까지 입력 가능합니다.")
    private String description;

    @Builder
    public PotCreateRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
