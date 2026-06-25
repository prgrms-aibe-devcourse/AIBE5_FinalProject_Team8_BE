package com.Rootin.domain.gamification.repository;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    interface PointSummaryProjection {
        Integer getCurrentPoint();
        Long getTotalEarned();
        Long getTotalUsed();
    }

    // 포인트 이력 목록 - 페이징
    Page<PointLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 기간별 포인트 합산 - 주간/월간 통계용
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointLog pl " +
            "WHERE pl.user.id = :userId AND pl.createdAt BETWEEN :from And :to")
    int sumAmountByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("from")LocalDateTime from,
            @Param("to") LocalDateTime to
            );

    // MT-01 오늘의 목표 - 오늘 지급된 퀘스트 reason 목록 단건 조회 (중복 지급 방지용)
    // createdAt BETWEEN 대신 awardedDate = :awardedDate 로 조회하여 datetime(6) microsecond 누락 문제 방지
    @Query("SELECT pl.reason FROM PointLog pl " +
            "WHERE pl.user.id = :userId " +
            "AND pl.awardedDate = :awardedDate " +
            "AND pl.reason IN :questReasons")
    Set<PointLogReason> findQuestReasonsByUserIdAndAwardedDate(
            @Param("userId") Long userId,
            @Param("awardedDate") LocalDate awardedDate,
            @Param("questReasons") Set<PointLogReason> questReasons
    );

    // MT-01 오늘의 목표 - 동시 요청에서 유니크 제약 충돌을 실패로 만들지 않고 먼저 성공한 1건만 지급 처리
    // MySQL 전용 쿼리입니다. INSERT IGNORE는 중복 키에서 0을 반환하므로 호출부가 첫 지급 여부를 안정적으로 구분할 수 있습니다.
    // uk_point_log_user_reason_date 유니크 인덱스가 중복 지급의 DB 레벨 방어선입니다.
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO point_log (user_id, reason, amount, awarded_date, created_at)
        VALUES (:userId, :reason, :amount, :awardedDate, NOW())
    """, nativeQuery = true)
    int insertQuestLogIfAbsent(
            @Param("userId") Long userId,
            @Param("reason") String reason,
            @Param("amount") int amount,
            @Param("awardedDate") LocalDate awardedDate
    );

    long countByUserIdAndReasonAndAwardedDate(Long userId, PointLogReason reason, LocalDate awardedDate);

    // MT-02 포인트 현황 - 현재 포인트와 적립/사용 합계를 한 번에 조회
    @Query(value = """
        SELECT
            u.point AS currentPoint,
            COALESCE(SUM(CASE WHEN pl.amount > 0 THEN pl.amount ELSE 0 END), 0) AS totalEarned,
            COALESCE(SUM(CASE WHEN pl.amount < 0 THEN -pl.amount ELSE 0 END), 0) AS totalUsed
        FROM users u
        LEFT JOIN point_log pl ON pl.user_id = u.id
        WHERE u.id = :userId
        GROUP BY u.id, u.point
    """, nativeQuery = true)
    java.util.Optional<PointSummaryProjection> summarizeByUserId(@Param("userId") Long userId);
}
