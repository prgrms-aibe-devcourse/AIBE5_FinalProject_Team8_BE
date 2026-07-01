package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.til.entity.PostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PotRepository extends JpaRepository<Pot, Long> {

    interface PotTilDistributionProjection {
        Long getPotId();
        String getPotTitle();
        Long getTilCount();
    }
    
    // 특정 사용자가 생성한 화분 목록을 조회하기 위한 메소드
    List<Pot> findByUserId(Long userId);

    @Query("""
        SELECT p.id AS potId,
               p.title AS potTitle,
               COUNT(t) AS tilCount
        FROM Pot p
        LEFT JOIN Til t ON t.pot = p AND t.user.id = :userId AND t.status = :status
        WHERE p.userId = :userId
        GROUP BY p.id, p.title
    """)
    List<PotTilDistributionProjection> findTilDistributionByUserId(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );

    // 동시 작성 시 화분 경험치/레벨의 유실을 방지하기 위한 비관적 락 조회 메소드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pot p WHERE p.id = :id")
    Optional<Pot> findByIdWithLock(@Param("id") Long id);
}
