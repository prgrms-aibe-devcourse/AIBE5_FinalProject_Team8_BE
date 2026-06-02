package com.Rootin.domain.collection.service;

import com.Rootin.domain.collection.dto.PlantCollectionItem;
import com.Rootin.domain.collection.dto.PlantCollectionResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final PlantRepository plantRepository;
    private final PlantItemRepository plantItemRepository;
    // 수집된 식물이 자라난 화분의 최신 이름을 동적으로 가져오기 위해 PotRepository를 주입합니다.
    private final PotRepository potRepository;

    public PlantCollectionResponse getPlants(Long userId) {
        // 도감에는 성장 단계별이 아닌, 식물 종류(종)별로 1개씩만 노출하기 위해 SEED(씨앗) 단계의 식물 마스터 정보만 필터링하여 가져옵니다.
        List<Plant> allPlants = plantRepository.findByGrowthStage(GrowthStage.SEED);
        // [식물도감 최초 수확 조회 최적화 - 팀원 공유용]
        // 기존에는 findByUserIdAndIsHarvestedTrue(userId)로 사용자의 전체 수확 이력 목록을 전부 다 어플리케이션 메모리로 가져와서
        // 스트림 집계로 최초 수확 건만 남겼습니다. 이 경우 오랜 사용으로 중복 수확 이력이 많아지면 메모리 부하와 네트워크 전하가 유발됩니다.
        // 이를 해결하기 위해 DB 레벨에서 서브쿼리를 사용하여 각 식물 종류(plantId)별로 가장 처음 수확(가장 이른 harvestedAt)된 레코드만
        // 딱 1건씩 선별적으로 O(종류 수) 규모로 벌크 조회해 오도록 튜닝을 반영했습니다.
        List<PlantItem> collected = plantItemRepository.findEarliestHarvestedPlantsByUserId(userId);

        // 1. N+1 쿼리 문제를 예방하기 위해, 수집 완료된 식물들이 속해있던 화분 ID들을 중복 없이 모읍니다.
        List<Long> potIds = collected.stream()
                .map(PlantItem::getPotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. 수집된 화분 ID 목록을 가지고 DB에서 단 1번의 IN 쿼리로 모든 화분을 일괄 조회하여 맵(potId -> potTitle) 형태로 가공합니다.
        Map<Long, String> potTitleMap = Map.of();
        if (!potIds.isEmpty()) {
            potTitleMap = potRepository.findAllById(potIds).stream()
                    .collect(Collectors.toMap(Pot::getId, Pot::getTitle));
        }

        // 3. 사용자가 동일 식물을 여러 번 수확했을 경우를 감안하여, 수집된 식물들 중 '최초 수집일(수확 시각이 가장 이른 것)' 기준의 식물 아이템을 찾아 맵으로 만듭니다.
        Map<Long, PlantItem> earliestCollectionMap = collected.stream()
                .collect(Collectors.toMap(
                        PlantItem::getPlantId,
                        item -> item,
                        (a, b) -> a.getHarvestedAt().isBefore(b.getHarvestedAt()) ? a : b
                ));

        // 4. 모든 도감 씨앗 리스트를 돌며 DTO로 변환하고 뱃지 정보를 조립합니다.
        final Map<Long, String> finalPotTitleMap = potTitleMap;
        List<PlantCollectionItem> plants = allPlants.stream()
                .map(plant -> {
                    // 해당 식물 종류의 최초 수집 정보를 맵에서 꺼냅니다.
                    PlantItem item = earliestCollectionMap.get(plant.getId());
                    boolean isCollected = (item != null);

                    String potName = null;
                    Integer harvestedLevel = null;
                    LocalDateTime collectedAt = null;

                    if (isCollected) {
                        collectedAt = item.getHarvestedAt();
                        harvestedLevel = item.getHarvestedLevel();
                        // 화분 ID가 유효하다면 위에서 일괄 조회한 화분 맵에서 현재 최신 화분명을 동적으로 매핑합니다.
                        // 화분이 영구 삭제되었거나 존재하지 않는다면 "알 수 없음"으로 채워줍니다.
                        if (item.getPotId() != null) {
                            potName = finalPotTitleMap.getOrDefault(item.getPotId(), "알 수 없음");
                        } else {
                            potName = "알 수 없음";
                        }
                    }

                    // 도감 화면에 필요한 뱃지 정보(현재 화분 이름, 수확 당시 레벨, 수확 시각 등)를 DTO 생성자에 함께 주입합니다.
                    return new PlantCollectionItem(
                            plant.getName(),
                            plant.getGrade() == Grade.RARE ? "희귀" : "일반",
                            isCollected,
                            collectedAt,
                            plant.getImageUrl(),
                            potName,
                            harvestedLevel
                    );
                })
                .collect(Collectors.toList());

        return new PlantCollectionResponse(plants);
    }
}
