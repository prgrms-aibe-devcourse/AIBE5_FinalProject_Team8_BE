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

    // 식물도감 — 사용자가 수확 완료한 식물 목록
    List<PlantItem> findByUserIdAndIsHarvestedTrue(Long userId);

    // 식물도감 — 현재 자라고 있는 식물 목록
    List<PlantItem> findByUserIdAndIsHarvestedFalse(Long userId);

    // 식물도감 — 사용자의 모든 식물 아이템
    List<PlantItem> findByUserId(Long userId);

    // 화분 내 식물 이력 (회차 계산용)
    List<PlantItem> findByPotIdOrderByIdAsc(Long potId);

    /**
     * 사용자가 수확한 식물들 중 식물 종류(plantId)별로 최초 수확일(가장 이른 harvestedAt)을 가진 레코드만 벌크로 조회합니다.
     * 도감 조회 시 사용자의 수많은 중복 수확 데이터가 모두 불러와져 발생하는 OOM 및 성능 저하 문제를 방지합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 각 식물 종류별 최초 수확 데이터 목록
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT pi FROM PlantItem pi
            WHERE pi.userId = :userId
              AND pi.isHarvested = true
              AND pi.harvestedAt = (
                  SELECT MIN(pi2.harvestedAt)
                  FROM PlantItem pi2
                  WHERE pi2.userId = :userId
                    AND pi2.plantId = pi.plantId
                    AND pi2.isHarvested = true
              )
            """)
    List<PlantItem> findEarliestHarvestedPlantsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
