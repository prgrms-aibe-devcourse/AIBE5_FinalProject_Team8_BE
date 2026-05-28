package com.Rootin.domain.garden.dto;

import java.time.LocalDateTime;

/**
 * 화분 상세 대시보드 화면(GD-03)을 그리기 위해 필요한 모든 정보를 묶어서 내려주는 복합 응답 DTO 레코드입니다.
 */
public record GardenInfoResponse(
    /**
     * 화분의 식별자 ID
     */
    Long potId,

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
     * 현재 레벨 구간 내에서 순수하게 획득해 채운 경험치
     */
    int currentLevelExp,

    /**
     * 다음 레벨로 올라가기 위해 필요한 이 구간에서의 목표 경험치
     */
    int nextLevelExpRequired,

    /**
     * 현재 레벨 구간 내에서의 성장 백분율 진행도 (0.0% ~ 100.0%)
     */
    double progressPercentage,

    /**
     * 사용자가 해당 화분에 작성한 총 발행 완료(PUBLISHED) TIL 게시글 수
     */
    long totalTilCount,

    /**
     * 사용자의 현재 연속 작성일 수 (오늘 작성 기록을 포함한 스트릭 누적 일수)
     */
    int streakDays,

    /**
     * 이 화분에 마지막으로 물을 준(TIL 경험치가 정산된) 일시
     */
    LocalDateTime lastWateredAt,

    /**
     * 현재 화분에 심겨 있는 식물의 성장 상태 마스터 정보 DTO
     */
    PlantInfoResponse plant
) {}
