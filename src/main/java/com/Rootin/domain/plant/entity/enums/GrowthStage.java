package com.Rootin.domain.plant.entity.enums;

import lombok.Getter;

/**
 * 식물의 성장 단계를 나타내는 ENUM 클래스입니다.
 * DB 스키마 상에 공백이 포함된 'Full Bloom'이 있기 때문에,
 * 자바 상수명은 표준 대문자 규칙(SEED, SPROUT 등)을 따르고 
 * 실제 DB 값(Seed, Sprout 등)은 내부 dbValue 변수로 관리합니다.
 */
@Getter
public enum GrowthStage {
    SEED("Seed"),
    SPROUT("Sprout"),
    MATURE("Mature"),
    BLOOM("Bloom"),
    FULL_BLOOM("Full Bloom");

    private final String dbValue;

    GrowthStage(String dbValue) {
        this.dbValue = dbValue;
    }

    public static GrowthStage fromDbValue(String dbValue) {
        for (GrowthStage stage : values()) {
            if (stage.getDbValue().equalsIgnoreCase(dbValue)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 식물 성장 단계 값입니다: " + dbValue);
    }
}
