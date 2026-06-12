package com.Rootin.domain.ai.repository;

import com.Rootin.domain.ai.entity.AiResultTil;
import com.Rootin.domain.ai.entity.AiResultTilId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AiResultTilRepository extends JpaRepository<AiResultTil, AiResultTilId> {

    // 파생 delete 쿼리(SELECT → N번 DELETE) 대신 단건 벌크 삭제로 성능 개선
    // orphanRemoval 우회에 따른 1차 캐시 불일치 위험 — TilService.delete()는 AiResult를
    // 로드하지 않으므로 현재는 안전하나, 같은 트랜잭션 내 AiResult 로드 추가 시 주의 필요
    @Transactional
    @Modifying
    @Query("DELETE FROM AiResultTil a WHERE a.til.id = :tilId")
    void deleteByTilId(@Param("tilId") Long tilId);
}
