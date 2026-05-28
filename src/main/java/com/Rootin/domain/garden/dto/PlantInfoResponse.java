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
    String silhouetteUrl
) {}
