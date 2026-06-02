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

@IntegrationTest
@Transactional
class CollectionServiceTest {

    @Autowired private CollectionService collectionService;
    @Autowired private UserRepository userRepository;
    @Autowired private PotRepository potRepository;
    @Autowired private PlantItemRepository plantItemRepository;
    @Autowired private PlantRepository plantRepository;

    private User testUser;
    private Pot testPot;
    private Plant commonPlant;
    private Plant rarePlant;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("도감사용자")
                .build();
        userRepository.save(testUser);

        testPot = Pot.builder()
                .userId(testUser.getId())
                .title("자바 정복 화분")
                .level(4)
                .totalExp(600)
                .build();
        potRepository.save(testPot);

        commonPlant = Plant.builder()
                .name("일반 장미").grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                .imageUrl("common_rose_url").build();
        plantRepository.save(commonPlant);

        rarePlant = Plant.builder()
                .name("희귀 선인장").grade(Grade.RARE).growthStage(GrowthStage.SEED)
                .imageUrl("rare_cactus_url").build();
        plantRepository.save(rarePlant);
    }

    @Test
    @DisplayName("식물 도감 조회 시 수확 완료 식물은 state=harvested, 미수집 식물은 state=locked로 구분되고 뱃지 정보(화분명, 수확 레벨, 수확일)가 정상 제공된다")
    void getPlantsCollectionBadgeSuccess() {
        // given
        LocalDateTime harvestTime = LocalDateTime.of(2026, 6, 1, 12, 0);
        PlantItem harvestedItem = PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(commonPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(4).harvestedAt(harvestTime)
                .build();
        plantItemRepository.save(harvestedItem);

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then
        assertThat(response).isNotNull();
        List<PlantCollectionItem> items = response.plants();
        assertThat(items).hasSize(2);

        // 수확 완료된 '일반 장미' 검증
        PlantCollectionItem commonItemDto = items.stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.state()).isEqualTo("harvested");
        assertThat(commonItemDto.rarity()).isEqualTo("common");
        assertThat(commonItemDto.harvestedAt()).isEqualTo(harvestTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        assertThat(commonItemDto.imageUrl()).isEqualTo("common_rose_url");
        assertThat(commonItemDto.potTitle()).isEqualTo("자바 정복 화분");
        assertThat(commonItemDto.potLevel()).isEqualTo(4); // harvestedLevel

        // 미수집 '희귀 선인장' 검증
        PlantCollectionItem rareItemDto = items.stream()
                .filter(i -> i.plantType().equals("희귀 선인장"))
                .findFirst().orElseThrow();
        assertThat(rareItemDto.state()).isEqualTo("locked");
        assertThat(rareItemDto.rarity()).isEqualTo("rare");
        assertThat(rareItemDto.harvestedAt()).isNull();
        assertThat(rareItemDto.potTitle()).isNull();
        assertThat(rareItemDto.potLevel()).isNull();
    }

    @Test
    @DisplayName("수집한 식물이 자랐던 화분이 삭제된 경우 도감 조회 시 화분명이 null로 안전하게 처리된다")
    void getPlantsCollectionDynamicPotDeletedFallback() {
        // given
        PlantItem harvestedItem = PlantItem.builder()
                .userId(testUser.getId()).potId(999L) // 존재하지 않는 화분 ID
                .plantId(commonPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(2).harvestedAt(LocalDateTime.now())
                .build();
        plantItemRepository.save(harvestedItem);

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then — 화분이 없어도 에러 없이 potTitle = null 처리
        PlantCollectionItem commonItemDto = response.plants().stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.state()).isEqualTo("harvested");
        assertThat(commonItemDto.potTitle()).isNull();
        assertThat(commonItemDto.potLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("동일 식물을 여러 번 수확한 경우 최초 수확 데이터가 도감에 대표로 표시된다")
    void getPlantsCollectionFirstEarnedRepresentative() {
        // given
        LocalDateTime earliestTime = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime laterTime   = LocalDateTime.of(2026, 6, 1, 15, 0);

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(commonPlant.getId())
                .isHarvested(true).harvestedLevel(2).harvestedAt(earliestTime).build());

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(commonPlant.getId())
                .isHarvested(true).harvestedLevel(4).harvestedAt(laterTime).build());

        // when
        PlantCollectionResponse response = collectionService.getPlants(testUser.getId());

        // then — 최초 수확(earliestTime, 레벨 2)이 대표 정보로 표시
        PlantCollectionItem commonItemDto = response.plants().stream()
                .filter(i -> i.plantType().equals("일반 장미"))
                .findFirst().orElseThrow();
        assertThat(commonItemDto.state()).isEqualTo("harvested");
        assertThat(commonItemDto.harvestedAt()).isEqualTo(earliestTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        assertThat(commonItemDto.potLevel()).isEqualTo(2);
    }
}
