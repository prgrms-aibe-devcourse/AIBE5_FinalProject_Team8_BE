package com.Rootin.domain.collection.service;

import com.Rootin.domain.collection.dto.PlantCollectionItem;
import com.Rootin.domain.collection.dto.PlantCollectionResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final PlantRepository plantRepository;
    private final PlantItemRepository plantItemRepository;
    private final PotRepository potRepository;
    private final WateringLogRepository wateringLogRepository;
    private final LevelCalculator levelCalculator;

    private static final Map<String, String> SPECIES_MAP = Map.of(
            "기본 씨앗",  "seed",
            "달빛씨앗",   "moonlight",
            "버섯씨앗",   "mushroom"
    );

    private static final Map<GrowthStage, String> STAGE_KEY = Map.of(
            GrowthStage.SEED,       "seed",
            GrowthStage.SPROUT,     "sprout",
            GrowthStage.MATURE,     "leaf",
            GrowthStage.BLOOM,      "bloom",
            GrowthStage.FULL_BLOOM, "full"
    );

    private static final int[]    THRESHOLDS  = {200, 500, 800, 1000};
    private static final String[] STAGE_NAMES = {"새싹", "잎", "개화", "만개"};

    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("MM.dd");
    private static final DateTimeFormatter LONG_FMT  = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public PlantCollectionResponse getPlants(Long userId) {
        Map<Long, Plant> plantMap = plantRepository.findAll().stream()
                .collect(Collectors.toMap(Plant::getId, p -> p));

        Map<Long, Pot> potMap = potRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Pot::getId, p -> p));

        List<PlantItem> allItems = plantItemRepository.findByUserId(userId);

        Map<Long, List<PlantItem>> itemsByPot = allItems.stream()
                .collect(Collectors.groupingBy(PlantItem::getPotId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparingLong(PlantItem::getId))
                                        .collect(Collectors.toList()))));

        Set<Long> encounteredPlantIds = new HashSet<>();
        List<PlantCollectionItem> growing   = new ArrayList<>();
        List<PlantCollectionItem> harvested = new ArrayList<>();

        List<PlantItem> sortedItems = allItems.stream()
                .sorted(Comparator.comparingInt(pi -> Boolean.TRUE.equals(pi.getIsHarvested()) ? 1 : 0))
                .collect(Collectors.toList());

        for (PlantItem item : sortedItems) {
            Plant plant = plantMap.get(item.getPlantId());
            if (plant == null) continue;
            encounteredPlantIds.add(item.getPlantId());

            Pot pot = potMap.get(item.getPotId());
            String rarity  = plant.getGrade() == Grade.RARE ? "rare" : "common";
            String species = SPECIES_MAP.getOrDefault(plant.getName(), "seed");

            List<PlantItem> potHistory = itemsByPot.getOrDefault(item.getPotId(), List.of());
            int round = potHistory.indexOf(item) + 1;

            List<WateringLog> logs = wateringLogRepository
                    .findByPotIdAndWateredAtGreaterThanEqualOrderByWateredAtAsc(
                            item.getPotId(), item.getCreatedAt());
            Map<String, String> stageDates = computeStageDates(logs, item);

            GrowthStage stage = levelCalculator.determinePlantGrowthStage(item.getGrowthExp());
            String currentStageKey = Boolean.TRUE.equals(item.getIsHarvested())
                    ? "full" : STAGE_KEY.getOrDefault(stage, "seed");

            // 수확된 식물은 수확 당시 레벨(harvestedLevel), 성장 중인 식물은 현재 레벨 사용
            boolean isHarvested = Boolean.TRUE.equals(item.getIsHarvested());
            Integer displayLevel = isHarvested
                    ? item.getHarvestedLevel()
                    : (pot != null ? pot.getLevel() : null);

            PlantCollectionItem entry = new PlantCollectionItem(
                    plant.getName(), species, rarity,
                    isHarvested ? "harvested" : "growing",
                    currentStageKey,
                    displayLevel,
                    pot != null ? pot.getTitle() : null,
                    round,
                    item.getCreatedAt().format(LONG_FMT),
                    item.getHarvestedAt() != null ? item.getHarvestedAt().format(LONG_FMT) : null,
                    stageDates,
                    plant.getImageUrl()
            );

            if (isHarvested) harvested.add(entry);
            else growing.add(entry);
        }

        List<PlantCollectionItem> locked = plantMap.values().stream()
                .filter(p -> !encounteredPlantIds.contains(p.getId()))
                .map(p -> new PlantCollectionItem(
                        p.getName(), SPECIES_MAP.getOrDefault(p.getName(), "seed"),
                        p.getGrade() == Grade.RARE ? "rare" : "common",
                        "locked", null, null, null, null, null, null, null, null))
                .collect(Collectors.toList());

        List<PlantCollectionItem> result = new ArrayList<>();
        result.addAll(growing);
        result.addAll(harvested);
        result.addAll(locked);
        return new PlantCollectionResponse(result);
    }

    private Map<String, String> computeStageDates(List<WateringLog> logs, PlantItem item) {
        Map<String, String> dates = new LinkedHashMap<>();
        dates.put("씨앗", item.getCreatedAt().format(SHORT_FMT));

        int cumExp  = 0;
        int cap     = item.getGrowthExp();
        int nextIdx = 0;

        for (WateringLog log : logs) {
            if (nextIdx >= THRESHOLDS.length || cumExp >= cap) break;
            int toAdd = Math.min(log.getExpGained(), cap - cumExp);
            cumExp += toAdd;
            while (nextIdx < THRESHOLDS.length && cumExp >= THRESHOLDS[nextIdx]) {
                dates.put(STAGE_NAMES[nextIdx], log.getWateredAt().format(SHORT_FMT));
                nextIdx++;
            }
        }

        for (int i = nextIdx; i < THRESHOLDS.length; i++) {
            dates.put(STAGE_NAMES[i], null);
        }
        return dates;
    }
}
