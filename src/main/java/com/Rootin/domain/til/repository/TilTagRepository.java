package com.Rootin.domain.til.repository;

import com.Rootin.domain.dashboard.dto.TagCountByPotDto;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.TilTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TilTagRepository extends JpaRepository<TilTag, Long> {

    // 관심사 분포용 - 사용자의 발행 TIL 기준 화분별 태그 빈도 집계
    // 화분 ID 오름차순, 태그 사용 횟수 내림차순으로 정렬하여 반환
    @Query("""
        SELECT tt.til.pot.id AS potId, tt.tag.name AS tagName, COUNT(tt) AS tagCount
        FROM TilTag tt
        WHERE tt.til.user.id = :userId AND tt.til.status = :status
        GROUP BY tt.til.pot.id, tt.tag.name
        ORDER BY tt.til.pot.id ASC, COUNT(tt) DESC
    """)
    List<TagCountByPotDto> findTagCountsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );
}
