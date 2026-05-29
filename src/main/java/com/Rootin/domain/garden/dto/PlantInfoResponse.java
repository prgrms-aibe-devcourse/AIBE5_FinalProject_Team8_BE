package com.Rootin.domain.garden.dto;

import com.Rootin.domain.plant.entity.enums.GrowthStage;

/**
 * 대시보드 화면에서 식물의 구체적인 외형 이미지 및 단계 정보를 프론트엔드로 전달하기 위한 DTO 레코드입니다.
 */
public record PlantInfoResponse(
    /**
     * 식물의 이름 (예: "기본 씨앗", "해바라기" 등)
     */
    String name,

    /**
     * 식물의 현재 성장 단계 Enum (SEED, SPROUT, MATURE, BLOOM, FULL_BLOOM)
     */
    GrowthStage growthStage,

    /**
     * 성장 단계에 따른 식물의 외형 이미지 URL
     */
    String imageUrl,

    /**
     * 도감용 실루엣 이미지 URL (잠겨 있는 단계 표시용)
     */
    String silhouetteUrl,

    /**
     * 식물의 성장 진행률 (전체 만개 기준 %, 소수점 첫째 자리까지)
     */
    double growthPercentage,

    /**
     * 수확 가능 여부 (식물 성장 경험치가 만개 기준에 도달했는지 여부)
     */
    boolean canHarvest
) {}
