package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.PlantCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantCollectionRepository extends JpaRepository<PlantCollection, Long> {

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndPlantId(Long userId, Long plantId);

    @Query("SELECT pc.plantId FROM PlantCollection pc WHERE pc.userId = :userId")
    List<Long> findPlantIdsByUserId(@Param("userId") Long userId);
}
