package com.Rootin.domain.garden.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TIL을 작성하여 화분에 물을 주었을 때의 상세 내역과 획득 경험치/포인트,
 * 그리고 계산 시점의 스트릭과 가중치 등의 통계를 영구적으로 보관하는 엔티티 클래스입니다.
 * H2/MySQL의 'watering_log' 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "watering_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WateringLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "exp_gained", nullable = false)
    private Integer expGained;

    @Column(name = "point_gained")
    private Integer pointGained;

    @Column(name = "content_length", nullable = false)
    private Integer contentLength;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays;

    @Column(name = "applied_multiplier", nullable = false)
    private Double appliedMultiplier;

    @Column(name = "before_pot_level", nullable = false)
    private Integer beforePotLevel;

    @Column(name = "after_pot_level", nullable = false)
    private Integer afterPotLevel;

    @Column(name = "before_total_exp", nullable = false)
    private Integer beforeTotalExp;

    @Column(name = "after_total_exp", nullable = false)
    private Integer afterTotalExp;

    @Column(name = "watered_at", nullable = false, updatable = false)
    private LocalDateTime wateredAt;

    @PrePersist
    protected void onCreate() {
        this.wateredAt = LocalDateTime.now();
    }

    @Builder
    public WateringLog(Long userId, Long potId, Long postId, Integer expGained, Integer pointGained,
                       Integer contentLength, Integer streakDays, Double appliedMultiplier,
                       Integer beforePotLevel, Integer afterPotLevel, Integer beforeTotalExp, Integer afterTotalExp) {
        this.userId = userId;
        this.potId = potId;
        this.postId = postId;
        this.expGained = expGained;
        this.pointGained = pointGained;
        this.contentLength = contentLength;
        this.streakDays = streakDays;
        this.appliedMultiplier = appliedMultiplier;
        this.beforePotLevel = beforePotLevel;
        this.afterPotLevel = afterPotLevel;
        this.beforeTotalExp = beforeTotalExp;
        this.afterTotalExp = afterTotalExp;
    }
}
