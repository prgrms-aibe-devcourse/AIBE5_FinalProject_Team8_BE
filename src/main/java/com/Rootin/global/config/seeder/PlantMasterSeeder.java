package com.Rootin.global.config.seeder;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlantMasterSeeder {

    static final String DEFAULT_PLANT_NAME = "기본 씨앗";

    private final PlantRepository plantRepository;

    public void seed() {
        boolean exists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .isPresent();
        if (exists) return;

        plantRepository.save(Plant.builder().name(DEFAULT_PLANT_NAME).grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
        plantRepository.save(Plant.builder().name("달빛씨앗").grade(Grade.RARE)
                .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
        plantRepository.save(Plant.builder().name("버섯씨앗").grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
        log.info("식물 마스터 데이터 저장 완료");
    }
}
