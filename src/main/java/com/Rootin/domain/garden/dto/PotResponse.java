package com.Rootin.domain.garden.dto;

import com.Rootin.domain.garden.entity.Pot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자님이 공유해주신 schema.md 속 'pot' 테이블의 전체 컬럼 구조를 온전히 리턴하기 위한 응답 DTO입니다.
 * id, user_id, title, description, level, total_exp, is_displayed, position_x, position_y, created_at
 * 컬럼들과 1:1로 정확하게 매핑됩니다.
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
