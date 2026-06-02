package com.Rootin.domain.plant.repository;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> {

    Optional<Plant> findFirstByNameAndGradeAndGrowthStage(String name, Grade grade, GrowthStage growthStage);

    List<Plant> findByGradeAndGrowthStage(Grade grade, GrowthStage growthStage);

    List<Plant> findByGrowthStage(GrowthStage growthStage);

    /**
     * 식물 이름 목록에 포함된 모든 식물 데이터를 한 번에 벌크로 조회합니다.
     * 정원(Garden) 조회 시 성장 단계별 이미지를 O(1) 인메모리 매핑으로 찾기 위해 사용됩니다.
     *
     * @param names 식물 이름 리스트 (예: ["해바라기", "장미"])
     * @return 해당 이름들을 가진 모든 단계/등급의 식물 엔티티 목록
     */
    List<Plant> findByNameIn(List<String> names);
}
