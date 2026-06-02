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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class HarvestService {

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final LevelCalculator levelCalculator;

    @Transactional
    public HarvestResponse harvest(Long userId, Long potId) {
        // 1. 동시 수확 요청 시 중복 씨앗 생성을 방지하기 위해 비관적 쓰기 락(Pessimistic Write Lock)을 사용해 화분을 조회합니다.
        Pot pot = potRepository.findByIdWithLock(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다."));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 접근할 권한이 없습니다.");
        }

        // 2. 현재 화분에 자라고 있는 활성 식물(isHarvested = false)을 조회합니다.
        PlantItem current = plantItemRepository.findByPotIdAndIsHarvestedFalse(potId)
                .orElseThrow(() -> CustomException.notFound("수확할 식물이 없습니다."));

        // 3. 만개 단계(경험치 1000) 달성 여부를 검증합니다.
        if (!levelCalculator.canHarvestPlant(current.getGrowthExp())) {
            throw CustomException.badRequest("아직 수확할 수 없습니다. 식물이 만개(경험치 1000) 단계에 도달해야 합니다.");
        }

        // 4. 수확 처리 (경험치 중복 연산을 피하고 화분에 저장된 최신 레벨 정보를 직접 활용하여 상태를 연동합니다)
        current.harvest(pot.getLevel());

        Plant harvestedPlant = plantRepository.findById(current.getPlantId())
                .orElseThrow(() -> CustomException.notFound("식물 마스터 데이터를 찾을 수 없습니다."));

        // 5. 다음 키울 새로운 랜덤 식물(씨앗 단계)을 선택하여 화분에 배정합니다.
        Plant nextPlant = selectRandomPlant();

        plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(potId)
                .plantId(nextPlant.getId())
                .build());

        return new HarvestResponse(
                harvestedPlant.getName(),
                harvestedPlant.getGrade() == Grade.RARE ? "희귀" : "일반",
                pot.getLevel(),
                nextPlant.getName(),
                nextPlant.getGrade() == Grade.RARE ? "희귀" : "일반"
        );
    }

    /**
     * 다음 화분에 배정할 임의의 식물 마스터(SEED 단계) 데이터를 선택합니다.
     */
    private Plant selectRandomPlant() {
        Grade grade = decideNextPlantGrade();

        List<Plant> candidates = plantRepository.findByGradeAndGrowthStage(grade, GrowthStage.SEED);
        if (candidates.isEmpty()) {
            // RARE 등급 식물이 데이터베이스에 존재하지 않으면 COMMON 등급 식물로 대체(Fallback)합니다.
            candidates = plantRepository.findByGradeAndGrowthStage(Grade.COMMON, GrowthStage.SEED);
        }
        if (candidates.isEmpty()) {
            throw CustomException.notFound("배정 가능한 식물 마스터 데이터가 없습니다.");
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /**
     * 다음 식물의 등급을 무작위로 선택합니다. (10% 확률로 RARE, 90% 확률로 COMMON)
     * 단위 테스트 시 난수 확률과 무관하게 RARE Fallback 분기를 100% 확정 검증할 수 있도록
     * Mockito Spy를 적용 가능하도록 protected 메소드로 추출했습니다.
     */
    protected Grade decideNextPlantGrade() {
        boolean isRare = ThreadLocalRandom.current().nextDouble() < 0.1;
        return isRare ? Grade.RARE : Grade.COMMON;
    }
}
