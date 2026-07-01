package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.TilTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TilTagRepository extends JpaRepository<TilTag, Long> {

    interface MonthlyTagCountProjection {
        String getMonth();
        String getTagName();
        long getTagCount();
    }

    @Query(value = """
        SELECT DATE_FORMAT(t.published_at, '%Y-%m') AS month,
               tg.name AS tagName,
               COUNT(*) AS tagCount
        FROM til_tag tt
        JOIN til t ON t.post_id = tt.til_id
        JOIN posts p ON p.id = t.post_id
        JOIN tag tg ON tg.id = tt.tag_id
        WHERE p.user_id = :userId
          AND p.status = :status
          AND t.published_at >= :from
        GROUP BY DATE_FORMAT(t.published_at, '%Y-%m'), tg.name
        ORDER BY month, tagCount DESC
    """, nativeQuery = true)
    List<MonthlyTagCountProjection> findMonthlyTagCountsSince(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("from") LocalDateTime from
    );

    // 퀘스트 Q2용 - 오늘 발행된 TIL에 붙은 태그 수 반환 (반열린 구간 [from, to))
    @Query("""
        SELECT COUNT(tt) FROM TilTag tt
        JOIN tt.til t
        WHERE t.user.id = :userId
        AND t.status = :status
        AND t.publishedAt >= :from
        AND t.publishedAt < :to
    """)
    long countByUserTodayTil(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TilTag tt WHERE tt.til.id IN :tilIds")
    int deleteByTilIdIn(@Param("tilIds") List<Long> tilIds);
}
