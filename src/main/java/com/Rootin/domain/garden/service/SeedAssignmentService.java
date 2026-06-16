package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.entity.PlantCollection;
import com.Rootin.domain.garden.repository.PlantCollectionRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 씨앗 배정 정책을 담당하는 서비스입니다.
 *
 * CASE A: 화분 생성 시
 *   - 첫 화분(해금 풀 비어있음) → 기본 씨앗 고정
 *   - 이후 화분 → 해금 풀(plant_collection) 내 등급별 랜덤
 *
 * CASE B: FULL_BLOOM 미만 수확 시
 *   - 해금 풀 내 등급별 랜덤 (풀 변화 없음)
 *
 * CASE C: FULL_BLOOM 수확 시
 *   - 전체 풀 랜덤 → 새 종이면 해금 풀에 추가
 *
 * 확률: RARE 해금 있으면 COMMON 90% / RARE 10%, 없으면 COMMON 100%
 * 등급 내에서는 해금된 종끼리 균등 분배
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeedAssignmentService {

    private final PlantCollectionRepository plantCollectionRepository;
    private final PlantRepository plantRepository;

    public Plant selectFromCollection(Long userId) {
        List<Long> plantIdsInCollection = plantCollectionRepository.findPlantIdsByUserId(userId);
        if (plantIdsInCollection.isEmpty()) {
            throw CustomException.notFound("해금된 씨앗이 없습니다.");
        }

        Grade grade = decideGrade();
        List<Plant> candidates = plantRepository.findByGradeAndGrowthStageAndIdIn(grade, GrowthStage.SEED, plantIdsInCollection);

        if (candidates.isEmpty() && grade == Grade.RARE) {
            candidates = plantRepository.findByGradeAndGrowthStageAndIdIn(Grade.COMMON, GrowthStage.SEED, plantIdsInCollection);
        }
        if (candidates.isEmpty()) {
            throw CustomException.notFound("해금된 씨앗이 없습니다.");
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public Plant selectFromAllPlants() {
        Grade grade = decideGrade();
        List<Plant> candidates = plantRepository.findByGradeAndGrowthStage(grade, GrowthStage.SEED);
        if (candidates.isEmpty()) {
            candidates = plantRepository.findByGradeAndGrowthStage(Grade.COMMON, GrowthStage.SEED);
        }
        if (candidates.isEmpty()) {
            throw CustomException.notFound("배정 가능한 식물 마스터 데이터가 없습니다.");
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    @Transactional
    public void addToCollectionIfNew(Long userId, Plant plant) {
        if (!plantCollectionRepository.existsByUserIdAndPlantId(userId, plant.getId())) {
            plantCollectionRepository.save(PlantCollection.builder()
                    .userId(userId)
                    .plantId(plant.getId())
                    .build());
        }
    }

    protected Grade decideGrade() {
        return ThreadLocalRandom.current().nextDouble() < 0.1 ? Grade.RARE : Grade.COMMON;
    }
}
