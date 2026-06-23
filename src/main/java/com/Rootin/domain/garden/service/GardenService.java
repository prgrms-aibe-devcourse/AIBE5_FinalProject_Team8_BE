package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.*;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GardenService {

    private final UserRepository userRepository;
    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final LevelCalculator levelCalculator;
    private final WateringLogRepository wateringLogRepository;

    /**
     * 로그인한 사용자의 정원 테마와 배치된(또는 배치 가능한) 화분 및 수확 식물 정보를 종합하여 반환합니다.
     */
    public GardenResponse getGarden(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다. ID: " + userId));

        List<Pot> pots = potRepository.findByUserId(userId);

        // 1. 화분 내 활성(수확 안 됨) 식물 아이템 매핑
        List<Long> potIds = pots.stream().map(Pot::getId).toList();
        final Map<Long, PlantItem> activePlantMap = potIds.isEmpty() ? java.util.Collections.emptyMap() :
                plantItemRepository.findByPotIdInAndIsHarvestedFalse(potIds).stream()
                        .collect(Collectors.toMap(PlantItem::getPotId, Function.identity(), (e1, e2) -> e1));

        // 2. 수확 완료된 식물 조회
        List<PlantItem> harvestedItems = plantItemRepository.findByUserIdAndIsHarvestedTrue(userId);

        // 3. 렌더링에 필요한 마스터 식물 데이터(Plant) 전체 벌크 조회
        List<Long> allPlantIds = Stream.concat(
                activePlantMap.values().stream().map(PlantItem::getPlantId),
                harvestedItems.stream().map(PlantItem::getPlantId)
        ).distinct().toList();

        Map<Long, Plant> masterPlantMap = java.util.Collections.emptyMap();
        if (!allPlantIds.isEmpty()) {
            masterPlantMap = plantRepository.findAllById(allPlantIds).stream()
                    .collect(Collectors.toMap(Plant::getId, Function.identity()));
        }

        // 4. 각 식물의 성장 단계별 이미지를 단일 쿼리로 조회하기 위해, 대상 식물 이름 목록을 추출합니다. (N+1 쿼리 방지)
        List<String> uniquePlantNames = masterPlantMap.values().stream()
                .map(Plant::getName)
                .distinct()
                .toList();

        // 5. 추출한 식물 이름들에 해당하는 모든 단계/등급의 식물 데이터를 단 한 번의 쿼리로 벌크 조회합니다.
        List<Plant> allStagePlants = uniquePlantNames.isEmpty() ? java.util.Collections.emptyList() :
                plantRepository.findByNameIn(uniquePlantNames);

        // 6. 빠른 O(1) 조회를 위해 Map<"이름_등급_성장단계", Plant> 구조로 가공합니다.
        Map<String, Plant> stagePlantMap = allStagePlants.stream()
                .collect(Collectors.toMap(
                        p -> generateStageKey(p.getName(), p.getGrade(), p.getGrowthStage()),
                        Function.identity(),
                        (p1, p2) -> p1 // 혹시 중복된 데이터가 감지되면 첫 번째 값을 보존합니다.
                ));

        // 7. 오늘 물을 준 화분 ID 목록을 조회합니다. (N+1 방지 벌크 조회)
        java.util.Set<Long> wateredPotIds = java.util.Collections.emptySet();
        if (!potIds.isEmpty()) {
            wateredPotIds = new java.util.HashSet<>(
                    wateringLogRepository.findWateredPotIdsToday(userId, potIds)
            );
        }

        // 8. 응답 DTO 조립
        final Map<Long, Plant> finalMasterPlantMap = masterPlantMap;
        final java.util.Set<Long> finalWateredPotIds = wateredPotIds;

        List<PotGardenResponse> potResponses = pots.stream().map(pot -> {
            PlantItem activeItem = activePlantMap.get(pot.getId());
            String plantName = "알 수 없음";
            GrowthStage growthStage = GrowthStage.SEED;
            String imageUrl = "";

            if (activeItem != null) {
                Plant master = finalMasterPlantMap.get(activeItem.getPlantId());
                if (master != null) {
                    plantName = master.getName();
                    // 식물의 누적 경험치(growthExp)를 통해 현재 성장 단계(씨앗, 새싹 등)를 계산합니다.
                    growthStage = levelCalculator.determinePlantGrowthStage(activeItem.getGrowthExp());

                    // 매번 DB 쿼리를 날리지 않고, 미리 벌크로 가져온 Map에서 O(1) 속도로 알맞은 단계의 식물 레코드를 조회합니다.
                    String stageKey = generateStageKey(master.getName(), master.getGrade(), growthStage);
                    Plant currentStagePlant = stagePlantMap.getOrDefault(stageKey, master);

                    imageUrl = currentStagePlant.getImageUrl();
                }
            }
            return new PotGardenResponse(
                    pot.getId(),
                    pot.getTitle(),
                    pot.getLevel(),
                    plantName,
                    growthStage,
                    imageUrl,
                    Boolean.TRUE.equals(pot.getIsDisplayed()),
                    pot.getPositionX(),
                    pot.getPositionY(),
                    finalWateredPotIds.contains(pot.getId())
            );
        }).toList();

        List<HarvestedPlantResponse> harvestedResponses = harvestedItems.stream().map(item -> {
            Plant master = finalMasterPlantMap.get(item.getPlantId());
            String plantName = "알 수 없음";
            String imageUrl = "";

            if (master != null) {
                plantName = master.getName();
                // 수확된 식물의 누적 경험치 정보를 통해 현재 성장 단계를 결정합니다.
                GrowthStage stage = levelCalculator.determinePlantGrowthStage(item.getGrowthExp());

                // 루프 안에서 매번 DB에 조회하는 대신, 메모리에 올려놓은 Map에서 식물 단계별 이미지를 O(1)로 가져옵니다.
                String stageKey = generateStageKey(master.getName(), master.getGrade(), stage);
                Plant currentStagePlant = stagePlantMap.getOrDefault(stageKey, master);

                imageUrl = currentStagePlant.getImageUrl();
            }

            return new HarvestedPlantResponse(
                    item.getId(),
                    item.getPlantId(),
                    plantName,
                    imageUrl,
                    Boolean.TRUE.equals(item.getIsDisplayed()),
                    item.getPositionX(),
                    item.getPositionY()
            );
        }).toList();

        return new GardenResponse(user.getGardenTheme(), potResponses, harvestedResponses);
    }

    /**
     * 식물의 이름, 등급, 성장 단계를 고유하게 결합하여 메모리 상의 O(1) 조회 Key를 생성합니다.
     */
    private String generateStageKey(String name, com.Rootin.domain.plant.entity.enums.Grade grade, GrowthStage stage) {
        if (name == null || grade == null || stage == null) {
            return "";
        }
        return name + "_" + grade.name() + "_" + stage.name();
    }


    /**
     * 정원 테마 업데이트
     */
    @Transactional
    public void updateGardenTheme(Long userId, GardenTheme theme) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        user.updateGardenTheme(theme);
    }

    /**
     * 화분과 수확 식물의 레이아웃 정보를 일괄 저장합니다.
     * findAllById를 이용한 벌크 조회 및 보안 검증(소유권 체크)이 포함되어 있습니다.
     */
    @Transactional
    public void updateGardenLayout(Long userId, GardenLayoutUpdateRequest request) {
        // 1. 화분(Pots) 일괄 업데이트
        if (request.getPots() != null && !request.getPots().isEmpty()) {
            List<Long> potIds = request.getPots().stream().map(LayoutUpdateDto::getId).toList();
            List<Long> distinctPotIds = potIds.stream().distinct().toList();

            // 요청에 동일한 화분 ID가 중복 전달된 경우, 클라이언트의 오동작으로 간주하여
            // 500 에러 대신 400 Bad Request 예외를 조기에 던져 시스템을 안전하게 방어합니다.
            if (potIds.size() != distinctPotIds.size()) {
                throw CustomException.badRequest("중복된 화분 ID가 요청에 포함되어 있습니다.");
            }

            List<Pot> pots = potRepository.findAllById(distinctPotIds);

            if (pots.size() != distinctPotIds.size()) {
                throw new CustomException(ErrorCode.POT_NOT_FOUND);
            }

            // Collectors.toMap() 연산 수행 시 중복 키로 인한 IllegalStateException(500 에러) 발생을 방지하기 위해
            // mergeFunction ((existing, replacement) -> existing)을 세 번째 파라미터로 명시적으로 제공합니다.
            Map<Long, LayoutUpdateDto> updateRequestMap = request.getPots().stream()
                    .collect(Collectors.toMap(
                            LayoutUpdateDto::getId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            for (Pot pot : pots) {
                if (!pot.getUserId().equals(userId)) {
                    throw CustomException.forbidden("타인 소유의 화분 배치 정보를 수정할 수 없습니다.");
                }
                LayoutUpdateDto dto = updateRequestMap.get(pot.getId());
                validateLayout(dto); // 서비스단 1차 유효성 방어
                pot.updateLayout(dto.getIsDisplayed(), dto.getPositionX(), dto.getPositionY()); // 엔티티 변경 감지
            }
        }

        // 2. 수확된 식물(HarvestedPlants) 일괄 업데이트
        if (request.getHarvestedPlants() != null && !request.getHarvestedPlants().isEmpty()) {
            List<Long> plantIds = request.getHarvestedPlants().stream().map(LayoutUpdateDto::getId).toList();
            List<Long> distinctPlantIds = plantIds.stream().distinct().toList();

            // 요청에 동일한 식물 ID가 중복 전달된 경우, 500 에러 방지를 위해 400 예외를 조기에 던집니다.
            if (plantIds.size() != distinctPlantIds.size()) {
                throw CustomException.badRequest("중복된 식물 ID가 요청에 포함되어 있습니다.");
            }

            List<PlantItem> plantItems = plantItemRepository.findAllById(distinctPlantIds);

            if (plantItems.size() != distinctPlantIds.size()) {
                throw CustomException.notFound("일부 수확 식물을 찾을 수 없거나 존재하지 않는 ID가 포함되어 있습니다.");
            }

            // Collectors.toMap() 연산 시 중복 키 충돌 예외를 원천적으로 예방하기 위해 mergeFunction을 적용합니다.
            Map<Long, LayoutUpdateDto> updateRequestMap = request.getHarvestedPlants().stream()
                    .collect(Collectors.toMap(
                            LayoutUpdateDto::getId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            for (PlantItem plantItem : plantItems) {
                if (!plantItem.getUserId().equals(userId)) {
                    throw CustomException.forbidden("타인 소유의 식물 배치 정보를 수정할 수 없습니다.");
                }
                if (!Boolean.TRUE.equals(plantItem.getIsHarvested())) {
                    throw CustomException.badRequest("아직 수확되지 않은 식물은 독립적으로 정원에 배치할 수 없습니다.");
                }

                LayoutUpdateDto dto = updateRequestMap.get(plantItem.getId());
                validateLayout(dto); // 서비스단 1차 유효성 방어
                plantItem.updateLayout(dto.getIsDisplayed(), dto.getPositionX(), dto.getPositionY());
            }
        }
    }

    /**
     * DTO 레벨에서의 1차 좌표 유효성 검증
     */
    private void validateLayout(LayoutUpdateDto dto) {
        if (Boolean.TRUE.equals(dto.getIsDisplayed())) {
            if (dto.getPositionX() == null || dto.getPositionX() < 0 ||
                dto.getPositionY() == null || dto.getPositionY() < 0) {
                throw CustomException.badRequest("정원에 배치(isDisplayed=true) 시 유효한 양수 좌표(X, Y)가 필수입니다.");
            }
        }
    }
}
