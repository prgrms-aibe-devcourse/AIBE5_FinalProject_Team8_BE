package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 화분 생성 및 조회에 관련된 핵심 비즈니스 로직을 담당하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotService {

    private static final String DEFAULT_PLANT_NAME = "기본 씨앗";

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;

    /**
     * 새로운 화분을 생성합니다.
     * 기획 명세에 따라, 화분 생성 성공 시 'plant_item' 테이블에도 기본 씨앗 정보가 함께 생성 매핑됩니다.
     */
    @Transactional
    public PotResponse createPot(Long userId, PotCreateRequest request) {
        Plant defaultPlant = getDefaultPlant();

        // 1. 화분 데이터 생성 및 저장
        Pot pot = Pot.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
        Pot savedPot = potRepository.save(pot);

        // 2. 화분에 심을 기본 씨앗 정보 매핑 저장
        PlantItem plantItem = PlantItem.builder()
                .userId(userId)
                .potId(savedPot.getId())
                .plantId(defaultPlant.getId())
                .build();
        plantItemRepository.save(plantItem);

        return PotResponse.from(savedPot);
    }

    private Plant getDefaultPlant() {
        return plantRepository.findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .orElseThrow(() -> CustomException.notFound("기본 식물 마스터 데이터가 존재하지 않습니다."));
    }

    /**
     * 특정 사용자가 보유한 모든 화분 목록을 조회합니다.
     */
    public List<PotResponse> getPots(Long userId) {
        return potRepository.findByUserId(userId).stream()
                .map(PotResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 화분의 상세 정보를 조회합니다.
     * 소유권 검증 및 CustomException을 적용했습니다.
     */
    public PotResponse getPot(Long potId, Long userId) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다. ID: " + potId));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 접근할 권한이 없습니다.");
        }

        return PotResponse.from(pot);
    }
}
