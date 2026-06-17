package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.dto.PotUpdateRequest;
import com.Rootin.domain.garden.entity.PlantCollection;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantCollectionRepository;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.PotTilCountProjection;
import com.Rootin.domain.ai.repository.AiResultRepository;
import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 화분 생성, 조회, 수정에 관련된 핵심 비즈니스 로직을 담당하는 서비스 클래스입니다.
 *
 * 이 클래스의 책임:
 * - 화분 생성
 * - 내 화분 목록 조회
 * - 화분 단건 조회
 * - 화분 제목/소개글 수정
 *
 * 대시보드처럼 여러 도메인의 데이터를 조합하는 복합 조회는 GardenDashboardService로 분리했습니다.
 * 식물 심기/교체처럼 PlantItem 선택 규칙이 포함되는 로직은 PotPlantService로 분리했습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotService {

    private static final String DEFAULT_PLANT_NAME = "기본 씨앗";

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final PlantCollectionRepository plantCollectionRepository;
    private final SeedAssignmentService seedAssignmentService;
    private final TilRepository tilRepository;
    private final AiResultRepository aiResultRepository;
    private final WateringLogRepository wateringLogRepository;
    private final LevelCalculator levelCalculator;

    /**
     * 새로운 화분을 생성합니다.
     * 기획 명세에 따라, 화분 생성 성공 시 'plant_item' 테이블에도 기본 씨앗 정보가 함께 생성 매핑됩니다.
     *
     * 흐름:
     * 1. Plant 마스터 테이블에서 "기본 씨앗 + COMMON + SEED" 데이터를 찾습니다.
     * 2. Pot을 저장합니다.
     * 3. 방금 만든 Pot에 기본 씨앗 PlantItem을 연결합니다.
     *
     * DEFAULT_PLANT_ID = 1L 같은 고정 ID를 쓰지 않는 이유:
     * - 개발 DB, 테스트 DB, 운영 DB마다 자동 증가 ID가 달라질 수 있습니다.
     * - 그래서 name/grade/growthStage라는 비즈니스 기준으로 기본 식물을 찾는 방식이 더 안전합니다.
     */
    @Transactional
    public PotResponse createPot(Long userId, PotCreateRequest request) {
        Plant selectedPlant = resolveInitialSeed(userId);

        Pot pot = Pot.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
        Pot savedPot = potRepository.save(pot);

        plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(savedPot.getId())
                .plantId(selectedPlant.getId())
                .build());

        return PotResponse.from(savedPot);
    }

    private Plant resolveInitialSeed(Long userId) {
        boolean hasCollection = plantCollectionRepository.existsByUserId(userId);
        if (!hasCollection) {
            // 첫 화분: 기본 씨앗 고정 후 해금 풀에 등록
            Plant defaultPlant = plantRepository.findFirstByNameAndGradeAndGrowthStage(
                            DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                    .orElseThrow(() -> CustomException.notFound("기본 식물 마스터 데이터가 존재하지 않습니다."));
            plantCollectionRepository.save(PlantCollection.builder()
                    .userId(userId)
                    .plantId(defaultPlant.getId())
                    .build());
            return defaultPlant;
        }
        // 이후 화분: 해금 풀 내 랜덤 배정
        return seedAssignmentService.selectFromCollection(userId);
    }

    /**
     * 특정 사용자가 보유한 모든 화분 목록을 요약 정보(PotSummaryResponse) DTO 목록으로 조회합니다.
     * 목록 화면에 필요한 화분의 경험치/레벨, 그리고 심겨진 식물의 이름 및 현재 성장 단계를 계산하여 제공합니다.
     * [성능 튜닝]: IN 절 쿼리와 메모리 내 Map 조립을 통해 2N+1번 발생하는 DB 조회를 단 5번으로 최적화하여 N+1 성능 이슈를 예방합니다.
     *
     * @param userId 사용자 ID
     * @return 요약된 화분 정보 목록
     */
    public List<PotSummaryResponse> getPots(Long userId) {
        List<Pot> pots = potRepository.findByUserId(userId);
        if (pots.isEmpty()) {
            return List.of();
        }

        // 1. 화분 ID 목록을 수집하여, IN 절 쿼리를 통해 수확되지 않은 PlantItem 목록을 한 번에 벌크 조회합니다. (쿼리 1회)
        List<Long> potIds = pots.stream().map(Pot::getId).toList();
        List<PlantItem> plantItems = plantItemRepository.findByPotIdInAndIsHarvestedFalse(potIds);

        // potId를 Key로 하는 Map으로 변환하여 메모리 내 탐색을 O(1)로 최적화합니다.
        // merge 함수(existing, duplicated) -> existing를 넣은 이유:
        // 데이터 오류로 한 화분에 활성 PlantItem이 2개 이상 생겨도 Collectors.toMap이 예외를 던지지 않도록 하기 위함입니다.
        // 이 경우 첫 번째 데이터를 사용하고, 근본적인 데이터 정합성 문제는 별도 운영/마이그레이션에서 정리해야 합니다.
        java.util.Map<Long, PlantItem> plantItemMap = plantItems.stream()
                .collect(Collectors.toMap(
                        PlantItem::getPotId,
                        java.util.function.Function.identity(),
                        (existing, duplicated) -> existing
                ));

        // 2. 조회된 PlantItem들의 plantId 목록을 추출해 마스터 식물(Plant) 목록을 한 번에 벌크 조회합니다. (findAllById -> IN 쿼리 기동, 쿼리 1회)
        List<Long> plantIds = plantItems.stream().map(PlantItem::getPlantId).distinct().toList();
        java.util.Map<Long, Plant> plantMap = java.util.Collections.emptyMap();
        if (!plantIds.isEmpty()) {
            List<Plant> plants = plantRepository.findAllById(plantIds);
            plantMap = plants.stream()
                    .collect(Collectors.toMap(Plant::getId, java.util.function.Function.identity()));
        }

        // 3. 화분 ID 목록으로 PUBLISHED TIL 수를 화분별로 벌크 집계합니다. (쿼리 1회)
        Map<Long, Integer> tilCountMap = tilRepository.countByPotIdInAndStatus(potIds, PostStatus.PUBLISHED)
                .stream()
                .collect(Collectors.toMap(
                        PotTilCountProjection::getPotId,
                        proj -> proj.getTilCount().intValue()
                ));

        // 4. 오늘 물을 준 화분 ID 목록을 조회합니다. (오늘 00:00:00 ~ 내일 00:00:00 기준)
        java.util.Set<Long> wateredPotIds = new java.util.HashSet<>(
                wateringLogRepository.findWateredPotIdsToday(userId, potIds)
        );

        // 5. 수집한 Map을 바탕으로 메모리 상에서 화분 정보와 식물 이름을 매핑하여 최종 DTO를 변환 반환합니다.
        final java.util.Map<Long, Plant> finalPlantMap = plantMap;
        return pots.stream()
                .map(pot -> {
                    PlantItem plantItem = plantItemMap.get(pot.getId());
                    String plantName = "알 수 없음";
                    int plantExp = 0;

                    if (plantItem == null) {
                        log.warn("[데이터 정합성 유실] 화분 조회 시 심겨진 식물(PlantItem)이 존재하지 않습니다. Pot ID: {}, Owner User ID: {}", pot.getId(), pot.getUserId());
                    } else {
                        plantExp = plantItem.getGrowthExp();
                        Plant plant = finalPlantMap.get(plantItem.getPlantId());
                        if (plant == null) {
                            log.warn("[데이터 정합성 유실] 식물 아이템(PlantItem ID: {})에 매핑된 마스터 식물 메타데이터(Plant ID: {})가 DB에 존재하지 않습니다. Pot ID: {}", plantItem.getId(), plantItem.getPlantId(), pot.getId());
                        } else {
                            plantName = plant.getName();
                        }
                    }

                    // 현재 식물의 경험치를 고려해 성장 단계 계산
                    GrowthStage growthStage = levelCalculator.determinePlantGrowthStage(plantExp);

                    return new PotSummaryResponse(
                            pot.getId(),
                            pot.getTitle(),
                            pot.getDescription(),
                            pot.getLevel(),
                            pot.getTotalExp(),
                            // DB 내에 is_displayed 값이 null인 경우 발생할 수 있는 NPE(NullPointerException)를
                            // 방지하기 위해 Boolean.TRUE.equals()를 사용하여 안전하게 기본값 false로 언박싱 변환합니다.
                            Boolean.TRUE.equals(pot.getIsDisplayed()),
                            plantName,
                            growthStage,
                            tilCountMap.getOrDefault(pot.getId(), 0),
                            wateredPotIds.contains(pot.getId())
                    );
                })
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

    @Transactional
    public PotResponse updatePot(Long potId, Long userId, PotUpdateRequest request) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다. ID: " + potId));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분을 수정할 권한이 없습니다.");
        }

        pot.updateInfo(request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null);

        return PotResponse.from(pot);
    }

    /**
     * 특정 화분을 삭제합니다.
     * 1. 화분 소유권 검증 (삭제 요청한 사용자가 화분의 소유자인지 확인)
     * 2. 화분에 속한 TIL 기반으로 작성된 AI 분석 결과(AiResult) 조회 및 삭제 처리
     *    (AiResult 삭제 시 cascade = CascadeType.ALL, orphanRemoval = true 설정에 의해 ai_result_til 중간 테이블 레코드도 자동 제거됨)
     * 3. 화분에 속한 모든 TIL 포스트 삭제 처리
     * 4. 화분에 심겨진 활성 식물(isHarvested=false) 삭제 처리 — 수확 완료 항목은 도감 기록 보존을 위해 제외
     * 5. 화분(Pot) 데이터 삭제
     */
    @Transactional
    public void deletePot(Long potId, Long userId) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다. ID: " + potId));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분을 삭제할 권한이 없습니다.");
        }

        // 1. 해당 화분과 연결된 AI 분석 결과(AiResult) 조회 및 일괄 삭제 처리
        List<AiResult> aiResults = aiResultRepository.findAllByPotId(potId);
        if (!aiResults.isEmpty()) {
            aiResultRepository.deleteAll(aiResults);
        }

        // 2. 해당 화분에 연결된 TIL 목록 조회 및 삭제 처리
        List<Til> tils = tilRepository.findByPotId(potId);
        if (!tils.isEmpty()) {
            // TIL 삭제 (JPA 상속/조인 구조에 따라 base post 테이블 및 til_tag 테이블은 cascade/자동 삭제됨)
            tilRepository.deleteAll(tils);
        }

        // 3. 해당 화분에 연결된 활성 식물 아이템(isHarvested=false/null)만 삭제 — 수확 완료 항목은 도감 보존
        List<PlantItem> plantItems = plantItemRepository.findActivePlantItemsByPotId(potId);
        if (!plantItems.isEmpty()) {
            plantItemRepository.deleteAll(plantItems);
        }

        // 4. 화분 데이터 삭제
        potRepository.delete(pot);
    }

    // S3에서 사용하기 위한 Validation
    public void validateOwnership(Long userId, Long potId) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 화분입니다. ID: " + potId));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 접근할 권한이 없습니다.");
        }
    }
}
