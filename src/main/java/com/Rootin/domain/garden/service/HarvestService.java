package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.HarvestResponse;
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

@Service
@RequiredArgsConstructor
public class HarvestService {

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final SeedAssignmentService seedAssignmentService;
    private final LevelCalculator levelCalculator;

    @Transactional
    public HarvestResponse harvest(Long userId, Long potId) {
        // 1. 동시 수확 요청 시 중복 씨앗 생성을 방지하기 위해 비관적 쓰기 락(Pessimistic Write Lock)을 사용해 화분을 조회합니다.
        Pot pot = potRepository.findByIdWithLock(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }

        // 2. 현재 화분에 자라고 있는 활성 식물(isHarvested = false)을 조회합니다.
        PlantItem current = plantItemRepository.findByPotIdAndIsHarvestedFalse(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.NO_ACTIVE_PLANT));

        // 3. 수확 시점의 성장 단계(0=씨앗 ~ 4=만개)를 계산합니다.
        int stageIndex = levelCalculator.determinePlantGrowthStage(current.getGrowthExp()).ordinal();

        // 4. 수확 처리 (경험치 중복 연산을 피하고 화분에 저장된 최신 레벨 정보를 직접 활용하여 상태를 연동합니다)
        current.harvest(pot.getLevel(), stageIndex);
        // IDENTITY 전략의 즉시 INSERT로 인해 ux_plant_item_one_active_per_pot 제약 위반을 막기 위해 먼저 플러시합니다.
        plantItemRepository.saveAndFlush(current);

        Plant harvestedPlant = plantRepository.findById(current.getPlantId())
                .orElseThrow(() -> CustomException.of(ErrorCode.PLANT_NOT_FOUND));

        // 5. 수확 단계에 따라 다음 씨앗 배정
        // FULL_BLOOM: 전체 풀 랜덤 + 새 종이면 해금 풀에 추가
        // 미만: 기존 해금 풀 내 랜덤 (풀 변화 없음)
        Plant nextPlant;
        if (stageIndex == GrowthStage.FULL_BLOOM.ordinal()) {
            nextPlant = seedAssignmentService.selectFromAllPlants();
            seedAssignmentService.addToCollectionIfNew(userId, nextPlant);
        } else {
            nextPlant = seedAssignmentService.selectFromCollection(userId);
        }

        plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(potId)
                .plantId(nextPlant.getId())
                .build());

        return new HarvestResponse(
                harvestedPlant.getName(),
                harvestedPlant.getGrade() == Grade.RARE ? "희귀" : "일반",
                pot.getLevel(),
                stageIndex,
                nextPlant.getName(),
                nextPlant.getGrade() == Grade.RARE ? "희귀" : "일반"
        );
    }
}
