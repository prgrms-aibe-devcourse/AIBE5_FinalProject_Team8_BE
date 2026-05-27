package com.Rootin.domain.garden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자님의 schema.md 속 'pot' 테이블 컬럼(title, description) 명세를 정확히 반영한 생성 DTO입니다.
 * 제목은 100자, 소개글은 255자 컬럼 길이에 맞추어 Validation을 적용했습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PotCreateRequest {

    @NotBlank(message = "화분 제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "화분 제목은 최대 100자까지 입력 가능합니다.")
    private String title;

    @Size(max = 255, message = "화분 소개글은 최대 255자까지 입력 가능합니다.")
    private String description;

    @Builder
    public PotCreateRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
