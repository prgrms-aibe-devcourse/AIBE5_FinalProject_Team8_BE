package com.Rootin.domain.collection.service;

import com.Rootin.domain.collection.dto.CollectionDexResponse;
import com.Rootin.domain.collection.dto.DexEntry;
import com.Rootin.domain.collection.dto.DexSection;
import com.Rootin.domain.collection.dto.DexStats;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final PlantRepository plantRepository;
    private final PlantItemRepository plantItemRepository;

    private static final DateTimeFormatter LONG_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final String[] STAGE_NAMES = {"씨앗", "새싹", "잎", "개화", "만개"};

    private static final Map<String, String> PLANT_NAME_TO_SPECIES = Map.of(
            "기본 씨앗",  "seed",
            "달빛씨앗",   "moon",
            "버섯씨앗",   "shroom",
            "선인장씨앗", "cactus",
            "불꽃씨앗",   "fire",
            "얼음씨앗",   "ice",
            "번개씨앗",   "bolt",
            "흑장미씨앗", "rose"
    );

    private record SpeciesMeta(
            String key,
            String label,
            boolean rare,
            String numRange,
            String[] monNames
    ) {}

    private static final List<SpeciesMeta> SPECIES_LIST = List.of(
            new SpeciesMeta("seed",   "씨앗몬 계열",   false, "001~005",
                    new String[]{"씨드몬","새싹몬","잎몬","꽃몬","나무왕"}),
            new SpeciesMeta("shroom", "버섯몬 계열",   false, "006~010",
                    new String[]{"포자씨","애버섯","버섯몬","독버섯몬","버섯대왕"}),
            new SpeciesMeta("cactus", "선인장몬 계열", false, "011~015",
                    new String[]{"가시씨","가시싹","선인장몬","꽃선인장","선인장왕"}),
            new SpeciesMeta("fire",   "불꽃몬 계열",   false, "016~020",
                    new String[]{"불씨몬","불싹몬","불꽃몬","화염몬","불꽃왕"}),
            new SpeciesMeta("ice",    "얼음몬 계열",   false, "021~025",
                    new String[]{"얼음씨","얼음싹","얼음몬","서리몬","빙하왕"}),
            new SpeciesMeta("moon",   "달빛씨앗 계열", true,  "026~030",
                    new String[]{"달빛씨","달빛싹","달빛잎","달빛꽃","달빛왕"}),
            new SpeciesMeta("bolt",   "번개씨앗 계열", true,  "031~035",
                    new String[]{"번개씨","번개싹","번개몬","뇌전몬","번개왕"}),
            new SpeciesMeta("rose",   "흑장미 계열",   true,  "036~040",
                    new String[]{"흑장미씨","흑장미싹","흑장미몬","흑장미꽃","흑장미왕"})
    );

    private static final int TOTAL     = 40;
    private static final int COMMON_CT = 25;
    private static final int RARE_CT   = 15;

    public CollectionDexResponse getPlants(Long userId) {
        List<Plant> allPlants = plantRepository.findAll();

        // id → Plant (수확 기록에서 종 판별용)
        Map<Long, Plant> plantById = allPlants.stream()
                .collect(Collectors.toMap(Plant::getId, p -> p));

        // speciesKey → (stageIndex → imageUrl) : GrowthStage ordinal = stageIndex (SEED=0 ~ FULL_BLOOM=4)
        Map<String, Map<Integer, String>> imageUrlBySpeciesStage = new HashMap<>();
        for (Plant plant : allPlants) {
            String speciesKey = PLANT_NAME_TO_SPECIES.get(plant.getName());
            if (speciesKey == null || plant.getImageUrl() == null) continue;
            imageUrlBySpeciesStage
                    .computeIfAbsent(speciesKey, k -> new HashMap<>())
                    .put(plant.getGrowthStage().ordinal(), plant.getImageUrl());
        }

        // 수확 완료된 식물들을 species × stageIndex별로 수집 (최초 수확 날짜 보관)
        List<PlantItem> harvestedItems = plantItemRepository.findByUserIdAndIsHarvestedTrue(userId);

        // speciesKey → (stageIndex → 최초 harvestedAt)
        Map<String, Map<Integer, LocalDateTime>> harvestDateBySpeciesStage = new HashMap<>();
        for (PlantItem item : harvestedItems) {
            Plant plant = plantById.get(item.getPlantId());
            if (plant == null) continue;
            String speciesKey = PLANT_NAME_TO_SPECIES.get(plant.getName());
            if (speciesKey == null || item.getHarvestedAt() == null) continue;
            // harvestedStageIndex가 null인 기존 데이터는 growthExp로 추정 (4 = FULL_BLOOM)
            int stageIndex = item.getHarvestedStageIndex() != null
                    ? item.getHarvestedStageIndex() : 4;
            harvestDateBySpeciesStage
                    .computeIfAbsent(speciesKey, k -> new HashMap<>())
                    .merge(stageIndex, item.getHarvestedAt(),
                            (existing, newer) -> existing.isBefore(newer) ? existing : newer);
        }

        int totalCollected = 0;
        List<DexSection> sections = new ArrayList<>();

        for (int si = 0; si < SPECIES_LIST.size(); si++) {
            SpeciesMeta meta = SPECIES_LIST.get(si);
            Map<Integer, LocalDateTime> stageDates = harvestDateBySpeciesStage.getOrDefault(meta.key(), Map.of());
            Map<Integer, String> stageImageUrls = imageUrlBySpeciesStage.getOrDefault(meta.key(), Map.of());

            List<DexEntry> entries = new ArrayList<>();
            for (int stage = 0; stage < 5; stage++) {
                boolean collected = stageDates.containsKey(stage);
                String dexNum = String.format("%03d", si * 5 + stage + 1);
                String harvestedAt = collected ? stageDates.get(stage).format(LONG_FMT) : null;
                String imageUrl = stageImageUrls.get(stage);

                entries.add(new DexEntry(
                        dexNum,
                        stage,
                        STAGE_NAMES[stage],
                        collected ? meta.monNames()[stage] : null,
                        collected,
                        harvestedAt,
                        imageUrl
                ));
                if (collected) totalCollected++;
            }

            sections.add(new DexSection(meta.key(), meta.label(), meta.rare(), meta.numRange(), entries));
        }

        return new CollectionDexResponse(new DexStats(TOTAL, COMMON_CT, RARE_CT, totalCollected), sections);
    }
}
