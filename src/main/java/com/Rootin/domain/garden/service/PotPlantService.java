package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.PlantOptionResponse;
import com.Rootin.domain.garden.dto.PlantingType;
import com.Rootin.domain.garden.dto.PotPlantOptionsResponse;
import com.Rootin.domain.garden.dto.PotPlantRequest;
import com.Rootin.domain.garden.dto.PotPlantResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotPlantService {

    private static final String GROWING_PLANT_BLOCK_REASON =
            "이미 성장 중인 식물이 있어 교체할 수 없습니다. 먼저 수확한 뒤 새 식물을 심어 주세요.";

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;

    public PotPlantOptionsResponse getPlantOptions(Long userId, Long potId) {
        Pot pot = getOwnedPot(userId, potId);

        List<PlantItem> activeItems = plantItemRepository.findActivePlantItemsByPotId(pot.getId());
        PlantItem currentPlantItem = activeItems.stream()
                .filter(item -> !isReplaceablePlaceholder(item))
                .findFirst()
                .orElseGet(() -> activeItems.stream().findFirst().orElse(null));
        boolean canPlant = activeItems.stream().allMatch(this::isReplaceablePlaceholder);

        List<PlantItem> harvestedPlantItems = getLatestHarvestedItemsByPlant(
                plantItemRepository.findByUserIdAndIsHarvestedTrue(userId)
        );
        Map<Long, Plant> plantMap = getPlantMap(currentPlantItem, harvestedPlantItems);
        List<PlantOptionResponse> harvestedPlants = harvestedPlantItems.stream()
                .map(item -> toPlantOptionResponse(item, plantMap.get(item.getPlantId())))
                .toList();

        return new PotPlantOptionsResponse(
                pot.getId(),
                canPlant,
                canPlant ? null : GROWING_PLANT_BLOCK_REASON,
                canPlant,
                currentPlantItem == null ? null : toPotPlantResponse(currentPlantItem, plantMap.get(currentPlantItem.getPlantId())),
                harvestedPlants
        );
    }

    @Transactional
    public PotPlantResponse plant(Long userId, Long potId, PotPlantRequest request) {
        Pot pot = potRepository.findByIdWithLock(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));
        validateOwner(pot, userId);

        List<PlantItem> activeItems = plantItemRepository.findActivePlantItemsByPotId(pot.getId());
        if (activeItems.stream().anyMatch(item -> !isReplaceablePlaceholder(item))) {
            throw CustomException.badRequest(GROWING_PLANT_BLOCK_REASON);
        }

        Plant selectedPlant = resolveSelectedSeedPlant(userId, request);

        if (!activeItems.isEmpty()) {
            plantItemRepository.deleteAll(activeItems);
            plantItemRepository.flush();
        }

        PlantItem plantedItem = plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(pot.getId())
                .plantId(selectedPlant.getId())
                .growthExp(0)
                .isHarvested(false)
                .build());

        return toPotPlantResponse(plantedItem, selectedPlant);
    }

    private Pot getOwnedPot(Long userId, Long potId) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));
        validateOwner(pot, userId);
        return pot;
    }

    private void validateOwner(Pot pot, Long userId) {
        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }
    }

    private boolean isReplaceablePlaceholder(PlantItem plantItem) {
        Integer growthExp = plantItem.getGrowthExp();
        return growthExp == null || growthExp == 0;
    }

    private Plant resolveSelectedSeedPlant(Long userId, PotPlantRequest request) {
        return switch (request.type()) {
            case RANDOM_SEED -> selectRandomSeedPlant();
            case HARVESTED_PLANT -> selectHarvestedPlantSeed(userId, request.sourcePlantItemId());
        };
    }

    private Plant selectHarvestedPlantSeed(Long userId, Long sourcePlantItemId) {
        if (sourcePlantItemId == null) {
            throw CustomException.badRequest("수확한 식물을 다시 심으려면 sourcePlantItemId가 필요합니다.");
        }

        PlantItem sourceItem = plantItemRepository.findById(sourcePlantItemId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 식물 아이템입니다. ID: " + sourcePlantItemId));
        if (!sourceItem.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 식물 아이템에 접근할 권한이 없습니다.");
        }
        if (!Boolean.TRUE.equals(sourceItem.getIsHarvested())) {
            throw CustomException.badRequest("수확 완료된 식물만 다시 심을 수 있습니다.");
        }

        Plant sourcePlant = plantRepository.findById(sourceItem.getPlantId())
                .orElseThrow(() -> CustomException.of(ErrorCode.PLANT_NOT_FOUND));

        if (sourcePlant.getGrowthStage() != GrowthStage.SEED) {
            throw CustomException.badRequest("수확 식물 아이템은 씨앗 단계 식물 마스터 데이터와 연결되어야 합니다.");
        }

        return sourcePlant;
    }

    private Plant selectRandomSeedPlant() {
        Grade grade = decideNextPlantGrade();

        List<Plant> candidates = plantRepository.findByGradeAndGrowthStage(grade, GrowthStage.SEED);
        if (candidates.isEmpty()) {
            candidates = plantRepository.findByGradeAndGrowthStage(Grade.COMMON, GrowthStage.SEED);
        }
        if (candidates.isEmpty()) {
            throw CustomException.of(ErrorCode.PLANT_NOT_FOUND);
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    protected Grade decideNextPlantGrade() {
        boolean isRare = ThreadLocalRandom.current().nextDouble() < 0.1;
        return isRare ? Grade.RARE : Grade.COMMON;
    }

    private List<PlantItem> getLatestHarvestedItemsByPlant(List<PlantItem> harvestedPlantItems) {
        return harvestedPlantItems.stream()
                .collect(Collectors.toMap(
                        PlantItem::getPlantId,
                        Function.identity(),
                        this::pickLatestHarvestedItem
                ))
                .values()
                .stream()
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .toList();
    }

    private PlantItem pickLatestHarvestedItem(PlantItem existing, PlantItem candidate) {
        if (existing.getHarvestedAt() == null) {
            return candidate;
        }
        if (candidate.getHarvestedAt() == null) {
            return existing;
        }
        return candidate.getHarvestedAt().isAfter(existing.getHarvestedAt()) ? candidate : existing;
    }

    private Map<Long, Plant> getPlantMap(PlantItem currentPlantItem, List<PlantItem> harvestedPlantItems) {
        Stream<PlantItem> currentPlantStream = currentPlantItem == null ? Stream.empty() : Stream.of(currentPlantItem);
        List<Long> plantIds = Stream.concat(currentPlantStream, harvestedPlantItems.stream())
                .map(PlantItem::getPlantId)
                .distinct()
                .toList();
        if (plantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return plantRepository.findAllById(plantIds).stream()
                .collect(Collectors.toMap(Plant::getId, Function.identity(), (existing, duplicated) -> existing));
    }

    private PotPlantResponse toPotPlantResponse(PlantItem plantItem, Plant plant) {
        if (plant == null) {
            throw CustomException.of(ErrorCode.PLANT_NOT_FOUND);
        }
        return new PotPlantResponse(
                plantItem.getPotId(),
                plantItem.getId(),
                plant.getId(),
                plant.getName(),
                toRarityKey(plant.getGrade()),
                plant.getGrowthStage(),
                plantItem.getGrowthExp()
        );
    }

    private PlantOptionResponse toPlantOptionResponse(PlantItem plantItem, Plant plant) {
        if (plant == null) {
            throw CustomException.of(ErrorCode.PLANT_NOT_FOUND);
        }
        return new PlantOptionResponse(
                plantItem.getId(),
                plant.getId(),
                plant.getName(),
                toRarityKey(plant.getGrade()),
                plant.getImageUrl(),
                plantItem.getHarvestedLevel(),
                plantItem.getHarvestedAt()
        );
    }

    private String toRarityKey(Grade grade) {
        return grade == Grade.RARE ? "rare" : "common";
    }
}
