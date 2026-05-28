package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface TilRepository extends JpaRepository<Til, Long> {

    Page<Til> findByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

    Page<Til> findByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t.publishedAt FROM Til t WHERE t.user.id = :userId AND t.status = :status ORDER BY t.publishedAt DESC")
    java.util.List<java.time.LocalDateTime> findPublishedAtByUserId(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("status") PostStatus status
    );

    Optional<Til> findFirstByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);
    // AI 서비스 전용 — 화분 내 전체 TIL 내용을 합산하여 OpenAI에 전달할 때 사용
    List<Til> findByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);

    /**
     * 특정 사용자가 소유한 특정 화분에 누적되어 발행 완료(PUBLISHED)된 TIL의 총 개수를 안전하게 계산합니다.
     * 대시보드에서 해당 화분에 작성된 총 TIL 통계를 보여줄 때 사용되며,
     * userId를 검증 조건에 포함하여 타인의 데이터 노출 및 오염을 방지합니다.
     *
     * @param userId 사용자 ID
     * @param potId 화분 ID
     * @param status 게시글 상태 (예: PostStatus.PUBLISHED)
     * @return 발행 완료된 TIL 개수
     */
    long countByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);

    // 총 TIL 개수
    long countByUserIdAndStatus(Long userId, PostStatus status);

    // 기간 내 TIL 개수 - 주간/월간 통계용
    long countByUserIdAndStatusAndPublishedAtBetween(
            Long userId,
            PostStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    // FD-05 주간 차트 - 기간 내 TIL 목록 (날짜별 집계용)
    List<Til> findByUserIdAndStatusAndPublishedAtBetween(
            Long userId,
            PostStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

}
