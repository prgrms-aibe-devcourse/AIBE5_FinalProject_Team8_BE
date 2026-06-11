package com.Rootin.domain.gamification.repository;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {

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

    // MT-02 포인트 현황 - 총 적립 포인트 (양수 합산)
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointLog pl " +
            "WHERE pl.user.id = :userId AND pl.amount > 0")
    int sumEarnedByUserId(@Param("userId") Long userId);

    // MT-02 포인트 현황 - 총 적립 포인트 (음수 합산)
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointLog pl " +
            "WHERE pl.user.id = :userId AND pl.amount < 0")
    int sumUsedByUserId(@Param("userId") Long userId);
}
