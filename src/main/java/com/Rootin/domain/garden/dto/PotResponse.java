package com.Rootin.domain.garden.dto;

import com.Rootin.domain.garden.entity.Pot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 클라이언트에 노출할 화분 기본 정보를 담는 응답 DTO입니다.
 * user_id는 소유권 검증용 내부 값이므로 응답에 포함하지 않고,
 * 화면에 필요한 id, title, description, level, totalExp, 배치 정보, 생성일만 내려줍니다.
 */
@Getter
public class PotResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final Integer level;
    private final Integer totalExp;
    private final Boolean isDisplayed;
    private final Integer positionX;
    private final Integer positionY;
    private final LocalDateTime createdAt;

    @Builder
    public PotResponse(Long id, String title, String description, Integer level, Integer totalExp, Boolean isDisplayed, Integer positionX, Integer positionY, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
        this.totalExp = totalExp;
        this.isDisplayed = isDisplayed;
        this.positionX = positionX;
        this.positionY = positionY;
        this.createdAt = createdAt;
    }

    public static PotResponse from(Pot pot) {
        return PotResponse.builder()
                .id(pot.getId())
                .title(pot.getTitle())
                .description(pot.getDescription())
                .level(pot.getLevel())
                .totalExp(pot.getTotalExp())
                .isDisplayed(pot.getIsDisplayed())
                .positionX(pot.getPositionX())
                .positionY(pot.getPositionY())
                .createdAt(pot.getCreatedAt())
                .build();
    }
}
