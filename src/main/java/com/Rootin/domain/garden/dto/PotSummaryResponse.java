package com.Rootin.domain.garden.dto;

import com.Rootin.domain.plant.entity.enums.GrowthStage;

/**
 * 화분 목록 화면(GD-04)을 간략하고 안전하게 그리기 위해 사용되는 화분 요약 정보 DTO 레코드입니다.
 * 기존 PotResponse가 너무 비대해지는 것을 방지하고, 목록 조회용으로 최적화하여 제공합니다.
 */
public record PotSummaryResponse(
    /**
     * 화분의 식별자 ID
     */
    Long id,

    /**
     * 화분의 제목
     */
    String title,

    /**
     * 화분의 소개 글
     */
    String description,

    /**
     * 화분의 현재 레벨
     */
    int level,

    /**
     * 화분의 전체 누적 경험치
     */
    int totalExp,

    /**
     * 메인 화면에 대표 화분으로 표시할지 여부 (NPE 예방을 위해 래퍼 클래스 Boolean 타입으로 선언)
     */
    Boolean isDisplayed,

    /**
     * 심겨 있는 식물의 마스터 이름 (예: "기본 씨앗")
     */
    String plantName,

    /**
     * 식물의 현재 성장 단계 (SEED, SPROUT 등)
     */
    GrowthStage growthStage,

    /**
     * 화분에 작성된 PUBLISHED 상태 TIL 총 개수
     */
    int tilCount,

    /**
     * 오늘 물을 주었는지 여부
     */
    boolean wateredToday
) {}
