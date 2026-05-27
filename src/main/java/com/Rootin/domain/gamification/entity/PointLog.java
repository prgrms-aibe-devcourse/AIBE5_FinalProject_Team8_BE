package com.Rootin.domain.gamification.entity;

import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포인트 사용/적립 내역 기록 엔티티
 * - amount 양수: 적립 (TIL 계열), 음수: 소모 (AI 계열)
 */
@Getter
@Entity
@Table(name = "point_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 포인트 변동 사유 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointLogReason reason;

    /** 변동량 — 양수: 적립, 음수: 소모 */
    @Column(nullable = false)
    private int amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public PointLog(User user, PointLogReason reason, int amount) {
        this.user = user;
        this.reason = reason;
        this.amount = amount;
    }
}
