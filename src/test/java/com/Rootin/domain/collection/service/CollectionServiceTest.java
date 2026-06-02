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
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 식물 도감 서비스(CollectionService)에 대한 비즈니스 로직 스프링 통합 테스트 클래스입니다.
 */
@IntegrationTest
@Transactional
class CollectionServiceTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private PlantItemRepository plantItemRepository;

    @Autowired
    private PlantRepository plantRepository;

    private User testUser;
    private Pot testPot;
    private Plant commonPlant;
    private Plant rarePlant;

    @BeforeEach
    void setUp() {
        // 암묵적 커밋을 유발하는 ALTER TABLE AUTO_INCREMENT DDL 구문을 제거했습니다.
        // 객체 ID를 동적으로 바인딩하여 롤백 무결성과 테스트 격리를 강화했습니다.

        // 1. 테스트용 사용자를 생성하고 저장합니다.
        testUser = User.builder()
                .email("test@example.com")
                .nickname("도감사용자")
                .build();
        userRepository.save(testUser);

        // 2. 테스트용 화분을 생성하고 저장합니다.
        testPot = Pot.builder()
                .userId(testUser.getId())
                .title("자바 정복 화분")
                .level(4)
                .totalExp(600)
                .build();
        potRepository.save(testPot);

        // 3. 테스트용 식물 마스터 데이터를 2개(일반, 희귀) 생성하고 저장합니다.
        // 도감에는 SEED 단계만 등록되므로 GrowthStage.SEED 단계로 설정합니다.
        commonPlant = Plant.builder()
                .name("일반 장미")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("common_rose_url")
                .build();
        plantRepository.save(commonPlant);

        rarePlant = Plant.builder()
                .name("희귀 선인장")
                .grade(Grade.RARE)
                .growthStage(GrowthStage.SEED)
                .imageUrl("rare_cactus_url")
                .build();
        plantRepository.save(rarePlant);
    }

    @Test
    @DisplayName("식물 도감을 조회하면 수집 완료된 식물과 미수집된 식물이 구분되어 출력되고 뱃지 정보(현재 화분명, 수확 레벨, 수확일)가 정상 제공된다")
    void getPlantsCollectionBadgeSuccess() {
        // given
        // 일반 장미를 수확 완료(isHarvested = true, 레벨 4, 수확시간 기록) 상태로 저장합니다.
        LocalDateTime harvestTime = LocalDateTime.of(2026, 6, 1, 12, 0);
        PlantItem harvestedItem = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(commonPlant.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(4)
                .harvestedAt(harvestTime)
                .build();
        plantItemRepository.save(harvestedItem);

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then
        assertThat(response).isNotNull();
        List<PlantCollectionItem> items = response.plants();
        assertThat(items).hasSize(2); // 마스터 식물 총 2종류

        // 1. 수집 완료된 '일반 장미'의 도감 데이터 및 뱃지 정보 검증
        PlantCollectionItem commonItemDto = items.stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.isCollected()).isTrue();
        assertThat(commonItemDto.rarity()).isEqualTo("일반");
        assertThat(commonItemDto.collectedAt()).isEqualTo(harvestTime);
        assertThat(commonItemDto.imageUrl()).isEqualTo("common_rose_url");
        assertThat(commonItemDto.currentPotName()).isEqualTo("자바 정복 화분"); // 현재 화분의 최신 이름 매핑 확인
        assertThat(commonItemDto.harvestedLevel()).isEqualTo(4);

        // 2. 수집되지 않은 '희귀 선인장'의 도감 데이터 및 null 상태 검증
        PlantCollectionItem rareItemDto = items.stream()
                .filter(i -> i.plantType().equals("희귀 선인장"))
                .findFirst().orElseThrow();
        assertThat(rareItemDto.isCollected()).isFalse();
        assertThat(rareItemDto.rarity()).isEqualTo("희귀");
        assertThat(rareItemDto.collectedAt()).isNull();
        assertThat(rareItemDto.currentPotName()).isNull();
        assertThat(rareItemDto.harvestedLevel()).isNull();
    }

    @Test
    @DisplayName("수집한 식물이 자랐던 화분이 삭제되었을 경우 도감 조회 시 화분명이 '알 수 없음'으로 안전하게 가드 처리된다")
    void getPlantsCollectionDynamicPotDeletedFallback() {
        // given
        // 존재하지 않는 화분 ID(999L) 혹은 삭제된 화분 ID로 수확된 식물 아이템을 세팅합니다.
        PlantItem harvestedItem = PlantItem.builder()
                .userId(testUser.getId())
                .potId(999L) // 존재하지 않는 화분 ID
                .plantId(commonPlant.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(2)
                .harvestedAt(LocalDateTime.now())
                .build();
        plantItemRepository.save(harvestedItem);

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then
        // 화분이 존재하지 않더라도 에러가 발생하지 않고 "알 수 없음" 처리가 정상적으로 이루어지는지 검증합니다.
        PlantCollectionItem commonItemDto = response.plants().stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.isCollected()).isTrue();
        assertThat(commonItemDto.currentPotName()).isEqualTo("알 수 없음");
        assertThat(commonItemDto.harvestedLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("동일한 식물을 여러 번 수확하여 중복 수집된 경우 최초 수집된 정보가 도감에 대표로 표시된다")
    void getPlantsCollectionFirstEarnedRepresentative() {
        // given
        // 동일한 일반 식물에 대해 수확 시점이 다른 2개의 수확 데이터를 생성합니다.
        LocalDateTime earliestTime = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime laterTime = LocalDateTime.of(2026, 6, 1, 15, 0);

        PlantItem firstHarvest = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(commonPlant.getId())
                .isHarvested(true)
                .harvestedLevel(2)
                .harvestedAt(earliestTime)
                .build();
        plantItemRepository.save(firstHarvest);

        PlantItem secondHarvest = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(commonPlant.getId())
                .isHarvested(true)
                .harvestedLevel(4)
                .harvestedAt(laterTime)
                .build();
        plantItemRepository.save(secondHarvest);

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then
        // 중복 수집되더라도 수집 시각이 더 이른 최초 수집 데이터(earliestTime, 레벨 2)가 대표 뱃지 정보로 매핑되는지 검증합니다.
        PlantCollectionItem commonItemDto = response.plants().stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.isCollected()).isTrue();
        assertThat(commonItemDto.collectedAt()).isEqualTo(earliestTime);
        assertThat(commonItemDto.harvestedLevel()).isEqualTo(2);
    }
}
