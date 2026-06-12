package com.Rootin.global.config.seeder;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlantMasterSeeder {

    static final String DEFAULT_PLANT_NAME = "기본 씨앗";

    private record PlantSpec(String name, Grade grade, String filePrefix) {}

    private static final List<PlantSpec> SPECS = List.of(
            new PlantSpec("기본 씨앗",  Grade.COMMON, "seed"),
            new PlantSpec("버섯씨앗",   Grade.COMMON, "mushroom"),
            new PlantSpec("선인장씨앗", Grade.COMMON, "cactus"),
            new PlantSpec("불꽃씨앗",   Grade.COMMON, "fire"),
            new PlantSpec("얼음씨앗",   Grade.COMMON, "ice"),
            new PlantSpec("달빛씨앗",   Grade.RARE,   "moonlight"),
            new PlantSpec("번개씨앗",   Grade.RARE,   "bolt"),
            new PlantSpec("흑장미씨앗", Grade.RARE,   "rose")
    );

    private static final GrowthStage[] STAGES = {
            GrowthStage.SEED, GrowthStage.SPROUT, GrowthStage.MATURE,
            GrowthStage.BLOOM, GrowthStage.FULL_BLOOM
    };

    private static final String[] STAGE_FILE_NAMES = {
            "01_seed", "02_sprout", "03_leaf", "04_flower", "05_bloom"
    };

    private final PlantRepository plantRepository;
    private final S3Service s3Service;

    public void seed() {
        boolean exists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .isPresent();
        if (exists) return;

        for (PlantSpec spec : SPECS) {
            for (int i = 0; i < STAGES.length; i++) {
                String imageUrl = s3Service.getFileUrl("plants/" + spec.filePrefix() + "/" + STAGE_FILE_NAMES[i] + ".svg");
                plantRepository.save(Plant.builder()
                        .name(spec.name())
                        .grade(spec.grade())
                        .growthStage(STAGES[i])
                        .imageUrl(imageUrl)
                        .silhouetteUrl(null)
                        .build());
            }
        }
        log.info("식물 마스터 데이터 저장 완료 (40종)");
    }
}
