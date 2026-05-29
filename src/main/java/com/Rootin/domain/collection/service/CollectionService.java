package com.Rootin.domain.collection.service;

import com.Rootin.domain.collection.dto.PlantCollectionItem;
import com.Rootin.domain.collection.dto.PlantCollectionResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final PlantRepository plantRepository;
    private final PlantItemRepository plantItemRepository;

    public PlantCollectionResponse getPlants(Long userId) {
        List<Plant> allPlants = plantRepository.findAll();
        List<PlantItem> collected = plantItemRepository.findByUserIdAndIsHarvestedTrue(userId);

        // plantId별로 가장 빠른 수확 시각 (최초 수집일)
        Map<Long, LocalDateTime> collectedAtMap = collected.stream()
                .collect(Collectors.toMap(
                        PlantItem::getPlantId,
                        PlantItem::getHarvestedAt,
                        (a, b) -> a.isBefore(b) ? a : b
                ));

        List<PlantCollectionItem> plants = allPlants.stream()
                .map(plant -> {
                    boolean isCollected = collectedAtMap.containsKey(plant.getId());
                    return new PlantCollectionItem(
                            plant.getName(),
                            plant.getGrade() == Grade.RARE ? "희귀" : "일반",
                            isCollected,
                            collectedAtMap.get(plant.getId()),
                            plant.getImageUrl()
                    );
                })
                .collect(Collectors.toList());

        return new PlantCollectionResponse(plants);
    }
}
