package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.PlantItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantItemRepository extends JpaRepository<PlantItem, Long> {
    
    // 특정 화분에 현재 심어져 있고 아직 수확되지 않은 식물 정보 조회
    Optional<PlantItem> findByPotIdAndIsHarvestedFalse(Long potId);
}
