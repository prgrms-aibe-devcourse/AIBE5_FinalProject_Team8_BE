package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.PlantItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantItemRepository extends JpaRepository<PlantItem, Long> {
    
    // 특정 화분에 현재 심어져 있고 아직 수확되지 않은 식물 정보 조회
    default Optional<PlantItem> findByPotIdAndIsHarvestedFalse(Long potId) {
        return findActivePlantItemsByPotId(potId).stream().findFirst();
    }

    @org.springframework.data.jpa.repository.Query("""
            SELECT pi FROM PlantItem pi
            WHERE pi.potId = :potId
              AND (pi.isHarvested = false OR pi.isHarvested IS NULL)
            ORDER BY pi.id ASC
            """)
    List<PlantItem> findActivePlantItemsByPotId(@org.springframework.data.repository.query.Param("potId") Long potId);

    /**
     * 여러 화분 ID 목록에 대해 현재 심어져 있고 수확되지 않은 식물(PlantItem) 목록을 IN 쿼리로 한 번에 벌크 조회합니다.
     * 화분 목록 조회 시 개별 쿼리가 N번 발생하는 N+1 문제를 방지하여 성능을 최적화하기 위해 사용합니다.
     *
     * @param potIds 조회할 화분 ID 목록
     * @return 수확되지 않은 식물 아이템 목록
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT pi FROM PlantItem pi
            WHERE pi.potId IN :potIds
              AND (pi.isHarvested = false OR pi.isHarvested IS NULL)
            """)
    List<PlantItem> findByPotIdInAndIsHarvestedFalse(@org.springframework.data.repository.query.Param("potIds") List<Long> potIds);
}
