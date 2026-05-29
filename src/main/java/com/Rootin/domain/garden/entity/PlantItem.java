package com.Rootin.domain.garden.entity;

import com.Rootin.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 특정 화분에 심어 키우고 있거나 수확 완료한 개별 식물 인스턴스 데이터를 관리하는 엔티티입니다.
 * garden 도메인(화분 및 가드닝 비즈니스 영역)의 패키지 구조에 매핑됩니다.
 */
@Entity
@Table(name = "plant_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlantItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "plant_id", nullable = false)
    private Long plantId;

    @Column(name = "growth_exp", nullable = false, columnDefinition = "int default 0")
    private Integer growthExp = 0;

    @Column(name = "harvested_level")
    private Integer harvestedLevel;

    @Column(name = "is_harvested")
    private Boolean isHarvested;

    @Column(name = "is_displayed")
    private Boolean isDisplayed;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "harvested_at")
    private LocalDateTime harvestedAt;

    @Builder
    public PlantItem(Long userId, Long potId, Long plantId, Integer growthExp, Integer harvestedLevel, Boolean isHarvested, Boolean isDisplayed, Integer positionX, Integer positionY, LocalDateTime harvestedAt) {
        this.userId = userId;
        this.potId = potId;
        this.plantId = plantId;
        this.growthExp = growthExp != null ? growthExp : 0;
        this.harvestedLevel = harvestedLevel;
        this.isHarvested = isHarvested != null ? isHarvested : false;
        this.isDisplayed = isDisplayed != null ? isDisplayed : false;
        this.positionX = positionX;
        this.positionY = positionY;
        this.harvestedAt = harvestedAt;
    }

    /**
     * 식물의 누적 경험치를 증가시킵니다.
     *
     * @param gainedExp 획득한 경험치 양
     */
    public void increaseGrowthExp(int gainedExp) {
        this.growthExp += gainedExp;
    }

    public void harvest(int potLevel) {
        this.isHarvested = true;
        this.harvestedAt = java.time.LocalDateTime.now();
        this.harvestedLevel = potLevel;
    }
}
