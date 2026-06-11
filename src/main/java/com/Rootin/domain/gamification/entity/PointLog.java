package com.Rootin.domain.gamification.entity;

import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 포인트 사용/적립 내역 기록 엔티티
 * - amount 양수: 적립 (퀘스트), 음수: 소모 (AI)
 *
 * 중복 지급 방지:
 * - QUEST 계열 reason은 awarded_date 를 오늘 날짜로 세팅하여 (user_id, reason, awarded_date) 유니크 제약으로 하루 1회 보장
 * - AI 계열 reason은 awarded_date = null → NULL != NULL 규칙으로 유니크 제약 적용 안 됨 (하루 여러 번 소모 가능)
 */
@Getter
@Entity
@Table(
    name = "point_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_point_log_user_reason_date",
        columnNames = {"user_id", "reason", "awarded_date"}
    )
)
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
    @Column(nullable = false, length = 50)
    private PointLogReason reason;

    /** 변동량 — 양수: 적립, 음수: 소모 */
    @Column(nullable = false)
    private int amount;

    /**
     * 퀘스트 포인트 지급 날짜 (QUEST 계열만 세팅, AI 계열은 null)
     * (user_id, reason, awarded_date) 유니크 제약의 날짜 축으로 사용
     */
    @Column(name = "awarded_date")
    private LocalDate awardedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public PointLog(User user, PointLogReason reason, int amount, LocalDate awardedDate) {
        this.user = user;
        this.reason = reason;
        this.amount = amount;
        this.awardedDate = awardedDate;
    }

    /**
     * QUEST 계열 포인트 지급 전용 팩토리 메서드.
     * awardedDate 를 반드시 지정하도록 강제하여 (user_id, reason, awarded_date) 유니크 제약이 올바르게 동작함을 보장합니다.
     *
     * @param user        수령 유저 (프록시 허용)
     * @param reason      QUEST_Q1 / QUEST_Q2 / QUEST_Q3 중 하나
     * @param amount      지급 포인트 (양수)
     * @param awardedDate 지급 날짜 (오늘, non-null)
     */
    public static PointLog forQuest(User user, PointLogReason reason, int amount, LocalDate awardedDate) {
        if (awardedDate == null) {
            throw new IllegalArgumentException("QUEST 계열 PointLog는 awardedDate가 필수입니다.");
        }
        return PointLog.builder()
                .user(user)
                .reason(reason)
                .amount(amount)
                .awardedDate(awardedDate)
                .build();
    }
}
