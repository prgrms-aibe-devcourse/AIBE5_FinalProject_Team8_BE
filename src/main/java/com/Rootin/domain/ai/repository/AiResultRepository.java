package com.Rootin.domain.ai.repository;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiResultRepository extends JpaRepository<AiResult, Long> {

    // 본인 전체 결과 조회
    List<AiResult> findAllByUser(User user);

    /**
     * 특정 화분 기준 필터링
     * ai_result_til → til.pot_id 경유
     */
    @Query("""
            SELECT DISTINCT ar FROM AiResult ar
            JOIN ar.aiResultTils art
            WHERE ar.user = :user
              AND art.til.id IN (
                  SELECT t.id FROM Til t WHERE t.pot.id = :potId
              )
            """)
    List<AiResult> findAllByUserAndPotId(@Param("user") User user, @Param("potId") Long potId);

    @Query("SELECT DISTINCT ar FROM AiResult ar JOIN ar.aiResultTils art WHERE art.til.pot.id = :potId")
    List<AiResult> findAllByPotId(@Param("potId") Long potId);
}
