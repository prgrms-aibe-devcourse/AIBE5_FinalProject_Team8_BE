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
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다."));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 접근할 권한이 없습니다.");
        }

        PlantItem current = plantItemRepository.findByPotIdAndIsHarvestedFalse(potId)
                .orElseThrow(() -> CustomException.notFound("수확할 식물이 없습니다."));

        if (!levelCalculator.canHarvestPlant(current.getGrowthExp())) {
            throw CustomException.badRequest("아직 수확할 수 없습니다. 식물이 만개(경험치 1000) 단계에 도달해야 합니다.");
        }

        // 수확 처리
        int currentLevel = levelCalculator.calculateLevel(pot.getTotalExp());
        current.harvest(currentLevel);

        Plant harvestedPlant = plantRepository.findById(current.getPlantId())
                .orElseThrow(() -> CustomException.notFound("식물 마스터 데이터를 찾을 수 없습니다."));

        // 랜덤 식물 선택 (RARE 10%, COMMON 90%)
        Plant nextPlant = selectRandomPlant();

        plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(potId)
                .plantId(nextPlant.getId())
                .build());

        return new HarvestResponse(
                harvestedPlant.getName(),
                harvestedPlant.getGrade() == Grade.RARE ? "희귀" : "일반",
                currentLevel,
                nextPlant.getName(),
                nextPlant.getGrade() == Grade.RARE ? "희귀" : "일반"
        );
    }

    private Plant selectRandomPlant() {
        boolean isRare = ThreadLocalRandom.current().nextDouble() < 0.1;
        Grade grade = isRare ? Grade.RARE : Grade.COMMON;

        List<Plant> candidates = plantRepository.findByGradeAndGrowthStage(grade, GrowthStage.SEED);
        if (candidates.isEmpty()) {
            // RARE 식물이 없으면 COMMON으로 폴백
            candidates = plantRepository.findByGradeAndGrowthStage(Grade.COMMON, GrowthStage.SEED);
        }
        if (candidates.isEmpty()) {
            throw CustomException.notFound("배정 가능한 식물 마스터 데이터가 없습니다.");
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
