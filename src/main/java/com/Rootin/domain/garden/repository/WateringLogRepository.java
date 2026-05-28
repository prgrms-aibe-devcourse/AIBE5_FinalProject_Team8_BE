package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.WateringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WateringLog 엔티티를 관리하기 위한 Spring Data JPA Repository 인터페이스입니다.
 */
@Repository
public interface WateringLogRepository extends JpaRepository<WateringLog, Long> {

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

    // 성장 이력 차트용 - 최근 30건
    List<WateringLog> findTop30ByUserIdOrderByWateredAtDesc(Long userId);

    // 활동 캘린더용 - 기간별 물주기 이력
    List<WateringLog> findByUserIdAndWateredAtBetween(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    );
}
