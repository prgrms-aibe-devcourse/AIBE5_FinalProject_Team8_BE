package com.Rootin.domain.garden.constant;

/**
 * 화분 도메인의 텍스트 길이 정책입니다.
 * DTO 검증과 JPA 컬럼 길이가 같은 기준을 바라보도록 이곳에서 관리합니다.
 */
public final class PotPolicy {

    public static final int TITLE_MAX_LENGTH = 10;
    public static final int DESCRIPTION_MAX_LENGTH = 25;

    private PotPolicy() {
    }
}
