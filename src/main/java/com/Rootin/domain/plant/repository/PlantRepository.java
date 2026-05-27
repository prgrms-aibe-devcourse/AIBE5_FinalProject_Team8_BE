package com.Rootin.domain.plant.repository;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> {

    Optional<Plant> findFirstByNameAndGradeAndGrowthStage(String name, Grade grade, GrowthStage growthStage);
}
