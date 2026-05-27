package com.Rootin.domain.plant.entity.enums;

import lombok.Getter;

/**
 * 식물의 희귀 등급을 정의하는 ENUM 클래스입니다.
 * 자바 상수명은 표준 대문자 규칙(RARE, COMMON)을 따르고
 * 실제 DB 값(Rare, Common)은 내부 dbValue 변수로 관리합니다.
 */
@Getter
public enum Grade {
    RARE("Rare"),
    COMMON("Common");

    private final String dbValue;

    Grade(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * DB에 저장된 문자열 값을 기준으로 매칭되는 Grade Enum 상수를 찾아서 반환합니다.
     * 대소문자를 구분하지 않고 유연하게 매칭합니다.
     */
    public static Grade fromDbValue(String dbValue) {
        for (Grade grade : values()) {
            if (grade.getDbValue().equalsIgnoreCase(dbValue)) {
                return grade;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 식물 희귀 등급 값입니다: " + dbValue);
    }
}
