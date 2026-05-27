package com.Rootin.global.config;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 초기 구동 시, 빈 데이터베이스에 기본 시스템 데이터(Seed Data)를 자동으로 넣어주는 컴포넌트입니다.
 * 특히, 화분(Pot) 생성 시 필수적으로 연동되어야 하는 기본 식물 마스터 데이터가
 * 누락되어 외래키(FK) 제약조건 오류가 발생하는 문제를 해결합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DatabaseSeeder {

    private static final String DEFAULT_PLANT_NAME = "기본 씨앗";

    private final PlantRepository plantRepository;

    /**
     * 스프링 컨텍스트 로딩이 완료되고 애플리케이션이 실행될 준비가 되었을 때(ApplicationReadyEvent) 기동됩니다.
     * 트랜잭션을 적용하여 예외 발생 시 안전하게 롤백되도록 구성했습니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        log.info("=== 애플리케이션 초기 구동: 데이터베이스 기본 시드 데이터(Seed Data) 검사 ===");

        boolean defaultPlantExists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .isPresent();

        if (!defaultPlantExists) {
            log.info("기본 식물 마스터 데이터가 존재하지 않습니다. 초기 Seed 데이터를 DB에 삽입합니다.");

            Plant defaultPlant = Plant.builder()
                    .name(DEFAULT_PLANT_NAME)
                    .grade(Grade.COMMON)
                    .growthStage(GrowthStage.SEED)
                    .imageUrl(null)
                    .silhouetteUrl(null)
                    .build();

            plantRepository.save(defaultPlant);
            log.info("기본 식물 마스터 데이터 저장 완료!");
        } else {
            log.info("기본 식물 마스터 데이터가 이미 존재합니다. 시딩을 건너뜁니다.");
        }

        log.info("=== 데이터베이스 기본 시드 데이터 검사 및 시딩 완료 ===");
    }
}
