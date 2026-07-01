package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.WateringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WateringLog 엔티티를 관리하기 위한 Spring Data JPA Repository 인터페이스입니다.
 */
@Repository
public interface WateringLogRepository extends JpaRepository<WateringLog, Long> {

    interface WateringLogDailyAggregateProjection {
        java.sql.Date getWateredDate();
        long getTilCount();
        long getContentLength();
    }

    interface WateringLogAggregateProjection {
        long getTilCount();
        long getContentLength();
    }

    interface DashboardPersonalOverviewProjection {
        long getTotalTilCount();
        long getTotalContentLength();
        Integer getCurrentPoints();
    }

    /**
     * 특정 화분에 기록된 모든 물주기 이력 목록을 조회합니다.
     *
     * @param potId 조회할 대상 화분 ID
     * @return 물주기 이력 리스트
     */
    List<WateringLog> findByPotId(Long potId);

    /**
     * 특정 TIL 포스트(postId)에 이미 물주기가 적용되었는지 여부를 확인합니다.
     *
     * @param postId 검사할 TIL 포스트 ID
     * @return 이미 존재하면 true, 없으면 false
     */
    boolean existsByPostId(Long postId);

    // TIL 삭제 시 연관 물주기 로그 일괄 제거
    @Transactional
    @Modifying
    @Query("DELETE FROM WateringLog w WHERE w.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    /**
     * 특정 사용자가 소유한 특정 화분의 물주기 로그 중 가장 최근에 물을 준 로그 1건을 안전하게 조회합니다.
     * 대시보드 화면에 해당 화분의 "마지막 물 준 시간"을 노출할 때 사용하며,
     * userId 조건을 조합하여 쿼리함으로써 잘못된 potId 매칭 리스크를 예방합니다.
     *
     * @param userId 사용자 ID
     * @param potId 화분 ID
     * @return 가장 최근의 물주기 로그 (존재하지 않을 수 있으므로 Optional 반환)
     */
    java.util.Optional<WateringLog> findFirstByUserIdAndPotIdOrderByWateredAtDesc(Long userId, Long potId);

    @Query(value = """
        SELECT watered_at
        FROM watering_log
        WHERE user_id = :userId
          AND pot_id = :potId
        ORDER BY watered_at DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<LocalDateTime> findLatestWateredAtByUserIdAndPotId(
            @Param("userId") Long userId,
            @Param("potId") Long potId
    );

    @Query(value = """
        SELECT
            (SELECT COUNT(*)
             FROM posts p
             JOIN til t ON t.post_id = p.id
             WHERE p.user_id = u.id
               AND p.status = :publishedStatus) AS totalTilCount,
            (SELECT COALESCE(SUM(w.content_length), 0)
             FROM watering_log w
             WHERE w.user_id = u.id) AS totalContentLength,
            u.point AS currentPoints
        FROM users u
        WHERE u.id = :userId
    """, nativeQuery = true)
    Optional<DashboardPersonalOverviewProjection> findPersonalOverviewByUserId(
            @Param("userId") Long userId,
            @Param("publishedStatus") String publishedStatus
    );

    @Query(value = """
        SELECT DISTINCT DATE(watered_at)
        FROM watering_log
        WHERE user_id = :userId
    """, nativeQuery = true)
    List<java.sql.Date> findDistinctWateredDatesByUserId(@Param("userId") Long userId);

    // 성장 이력 차트용 - 최근 30건
    List<WateringLog> findTop30ByUserIdOrderByWateredAtDesc(Long userId);

    @Query(value = """
        SELECT DATE(watered_at) AS wateredDate,
               COUNT(*) AS tilCount,
               COALESCE(SUM(content_length), 0) AS contentLength
        FROM watering_log
        WHERE user_id = :userId
          AND watered_at >= :from
          AND watered_at < :to
        GROUP BY DATE(watered_at)
        ORDER BY wateredDate
    """, nativeQuery = true)
    List<WateringLogDailyAggregateProjection> aggregateDailyByUserIdAndWateredAtRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    long countByUserIdAndWateredAtGreaterThanEqualAndWateredAtLessThan(Long userId, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(w) AS tilCount,
               COALESCE(SUM(w.contentLength), 0) AS contentLength
        FROM WateringLog w
        WHERE w.userId = :userId
          AND w.wateredAt >= :from
          AND w.wateredAt < :to
    """)
    Optional<WateringLogAggregateProjection> aggregateByUserIdAndWateredAtGreaterThanEqualAndWateredAtLessThan(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 식물 성장 단계 날짜 계산용 — 특정 시점 이후 해당 화분의 물주기 이력 (시간순)
    List<WateringLog> findByPotIdAndWateredAtGreaterThanEqualOrderByWateredAtAsc(
            Long potId,
            LocalDateTime from
    );

    @Query("""
        SELECT DISTINCT w.potId
        FROM WateringLog w
        WHERE w.userId = :userId
          AND w.potId IN :potIds
          AND w.wateredAt >= :startOfDay
          AND w.wateredAt < :startOfNextDay
    """)
    List<Long> findWateredPotIds(
        @Param("userId") Long userId,
        @Param("potIds") List<Long> potIds,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("startOfNextDay") LocalDateTime startOfNextDay
    );

    default List<Long> findWateredPotIdsToday(Long userId, List<Long> potIds) {
        if (potIds == null || potIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        return findWateredPotIds(userId, potIds, startOfDay, startOfNextDay);
    }
}
