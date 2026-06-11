package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.TilTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TilTagRepository extends JpaRepository<TilTag, Long> {

    // 관심사 흐름(interests) 조회용 - 기준 시점 이후 발행된 TIL의 태그 전체 반환 (Java 측에서 월별 집계)
    @Query("""
        SELECT tt FROM TilTag tt
        JOIN FETCH tt.tag
        JOIN FETCH tt.til t
        WHERE t.user.id = :userId
        AND t.status = :status
        AND t.publishedAt >= :from
    """)
    List<TilTag> findTagsSince(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            @Param("from") LocalDateTime from
    );

    // 퀘스트 Q2용 - 오늘 발행된 TIL에 붙은 태그 수 반환
    @Query("""
        SELECT COUNT(tt) FROM TilTag tt
        JOIN tt.til t
        WHERE t.user.id = :userId
        AND t.status = :status
        AND t.publishedAt >= :from
        AND t.publishedAt <= :to
    """)
    long countByUserTodayTil(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
