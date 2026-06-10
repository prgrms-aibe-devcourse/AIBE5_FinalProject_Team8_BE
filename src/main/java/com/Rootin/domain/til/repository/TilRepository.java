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

    List<Til> findByPotId(Long potId);

    @Query("SELECT t.publishedAt FROM Til t WHERE t.user.id = :userId AND t.status = :status ORDER BY t.publishedAt DESC")
    List<LocalDateTime> findPublishedAtByUserId(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );

    Optional<Til> findFirstByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);

    // AI 서비스 전용 - 화분 내 전체 TIL 합산용
    List<Til> findByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);

    // AI 서비스 전용 - tilIds 선택 시 본인 소유 + PUBLISHED TIL만 DB 레벨에서 필터
    @Query("SELECT t FROM Til t WHERE t.id IN :ids AND t.status = :status AND t.user.id = :userId")
    List<Til> findAllByIdInAndStatusAndUserId(
            @Param("ids") List<Long> ids,
            @Param("status") PostStatus status,
            @Param("userId") Long userId
    );

    @Query(
        value = "SELECT DISTINCT t FROM Til t" +
                " LEFT JOIN t.tilTags tt LEFT JOIN tt.tag tg" +
                " WHERE t.user.id = :userId AND t.status = :status" +
                " AND (:potId IS NULL OR t.pot.id = :potId)" +
                " AND (:keyword IS NULL OR t.title LIKE %:keyword%)" +
                " AND (:tag IS NULL OR tg.name = :tag)",
        countQuery = "SELECT COUNT(DISTINCT t.id) FROM Til t" +
                " LEFT JOIN t.tilTags tt LEFT JOIN tt.tag tg" +
                " WHERE t.user.id = :userId AND t.status = :status" +
                " AND (:potId IS NULL OR t.pot.id = :potId)" +
                " AND (:keyword IS NULL OR t.title LIKE %:keyword%)" +
                " AND (:tag IS NULL OR tg.name = :tag)"
    )
    Page<Til> findByFilters(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            @Param("potId") Long potId,
            @Param("keyword") String keyword,
            @Param("tag") String tag,
            Pageable pageable
    );

    long countByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status);

    @Query("SELECT t.pot.id AS potId, COUNT(t) AS tilCount FROM Til t WHERE t.pot.id IN :potIds AND t.status = :status GROUP BY t.pot.id")
    List<PotTilCountProjection> countByPotIdInAndStatus(@Param("potIds") List<Long> potIds, @Param("status") PostStatus status);

    long countByUserIdAndStatus(Long userId, PostStatus status);

    long countByUserIdAndStatusAndPublishedAtBetween(Long userId, PostStatus status, LocalDateTime from, LocalDateTime to);
}
