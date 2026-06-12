package com.Rootin.domain.til.repository;

/**
 * 화분별 PUBLISHED TIL 수 벌크 집계 쿼리 결과를 타입 안전하게 매핑하기 위한 프로젝션 인터페이스입니다.
 */
public interface PotTilCountProjection {
    Long getPotId();
    Long getTilCount();
}
