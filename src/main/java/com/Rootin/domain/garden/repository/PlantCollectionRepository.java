package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.PlantCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantCollectionRepository extends JpaRepository<PlantCollection, Long> {

    List<PlantCollection> findByUserId(Long userId);

    boolean existsByUserIdAndPlantId(Long userId, Long plantId);
}
