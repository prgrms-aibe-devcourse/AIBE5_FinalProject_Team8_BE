package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.PlantOptionResponse;
import com.Rootin.domain.garden.dto.PlantingType;
import com.Rootin.domain.garden.dto.PotPlantOptionsResponse;
import com.Rootin.domain.garden.dto.PotPlantRequest;
import com.Rootin.domain.garden.dto.PotPlantResponse;
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
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@IntegrationTest
@Transactional
class PotPlantServiceTest {

    @SpyBean
    private PotPlantService potPlantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private PlantItemRepository plantItemRepository;

    @Autowired
    private PlantRepository plantRepository;

    private User testUser;
    private User otherUser;
    private Pot testPot;
    private Plant defaultSeed;
    private Plant mushroomSeed;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("pot-plant-user@test.com")
                .nickname("화분심기유저")
                .build());
        otherUser = userRepository.save(User.builder()
                .email("pot-plant-other@test.com")
                .nickname("다른심기유저")
                .build());

        testPot = potRepository.save(Pot.builder()
                .userId(testUser.getId())
                .title("테스트 화분")
                .description("식물 교체 테스트용 화분")
                .level(7)
                .totalExp(700)
                .build());

        defaultSeed = plantRepository.save(Plant.builder()
                .name("기본 씨앗")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("default_seed.png")
                .build());
        mushroomSeed = plantRepository.save(Plant.builder()
                .name("버섯씨앗")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("mushroom_seed.png")
                .build());
    }

    @Test
    @DisplayName("현재 식물이 0 EXP 씨앗 상태이면 수확한 식물 종류를 새로 심을 수 있고, 수확 이력은 유지된다")
    void plantHarvestedPlantSuccess() {
        // given
        PlantItem currentPlaceholder = plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(defaultSeed.getId())
                .growthExp(0)
                .isHarvested(false)
                .build());
        PlantItem harvestedPlant = plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(mushroomSeed.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(7)
                .harvestedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(mushroomSeed.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(3)
                .harvestedAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build());

        // when
        PotPlantResponse response = potPlantService.plant(
                testUser.getId(),
                testPot.getId(),
                new PotPlantRequest(PlantingType.HARVESTED_PLANT, harvestedPlant.getId())
        );

        // then
        assertThat(response.potId()).isEqualTo(testPot.getId());
        assertThat(response.plantName()).isEqualTo("버섯씨앗");
        assertThat(response.growthExp()).isEqualTo(0);

        assertThat(plantItemRepository.findById(currentPlaceholder.getId())).isEmpty();

        PlantItem sourceStillHarvested = plantItemRepository.findById(harvestedPlant.getId()).orElseThrow();
        assertThat(sourceStillHarvested.getIsHarvested()).isTrue();
        assertThat(sourceStillHarvested.getHarvestedLevel()).isEqualTo(7);

        List<PlantItem> activeItems = plantItemRepository.findActivePlantItemsByPotId(testPot.getId());
        assertThat(activeItems).hasSize(1);
        assertThat(activeItems.get(0).getPlantId()).isEqualTo(mushroomSeed.getId());
        assertThat(activeItems.get(0).getGrowthExp()).isEqualTo(0);
    }

    @Test
    @DisplayName("현재 식물이 성장 중이면 새 식물을 심을 수 없고 BAD_REQUEST 예외가 발생한다")
    void plantFailWhenCurrentPlantIsGrowing() {
        // given
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(defaultSeed.getId())
                .growthExp(10)
                .isHarvested(false)
                .build());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                potPlantService.plant(
                        testUser.getId(),
                        testPot.getId(),
                        new PotPlantRequest(PlantingType.RANDOM_SEED, null)
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("이미 성장 중인 식물");
    }

    @Test
    @DisplayName("심기 선택지 조회 시 성장 중인 식물이 있으면 canPlant=false와 사유를 함께 반환한다")
    void getPlantOptionsBlockedByGrowingPlant() {
        // given
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(defaultSeed.getId())
                .growthExp(300)
                .isHarvested(false)
                .build());
        PlantItem harvestedPlant = plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(mushroomSeed.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(7)
                .harvestedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build());

        // when
        PotPlantOptionsResponse response = potPlantService.getPlantOptions(testUser.getId(), testPot.getId());

        // then
        assertThat(response.canPlant()).isFalse();
        assertThat(response.randomSeedAvailable()).isFalse();
        assertThat(response.unavailableReason()).contains("이미 성장 중인 식물");
        assertThat(response.currentPlant()).isNotNull();
        assertThat(response.currentPlant().growthExp()).isEqualTo(300);
        assertThat(response.harvestedPlants()).extracting(PlantOptionResponse::sourcePlantItemId)
                .containsExactly(harvestedPlant.getId());
    }

    @Test
    @DisplayName("타인의 수확 식물을 sourcePlantItemId로 넘기면 FORBIDDEN 예외가 발생한다")
    void plantFailWhenSourcePlantBelongsToOtherUser() {
        // given
        PlantItem otherHarvestedPlant = plantItemRepository.save(PlantItem.builder()
                .userId(otherUser.getId())
                .potId(testPot.getId())
                .plantId(mushroomSeed.getId())
                .growthExp(1000)
                .isHarvested(true)
                .build());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                potPlantService.plant(
                        testUser.getId(),
                        testPot.getId(),
                        new PotPlantRequest(PlantingType.HARVESTED_PLANT, otherHarvestedPlant.getId())
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getMessage()).contains("해당 식물 아이템에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("랜덤 씨앗 심기에서 RARE 데이터가 없으면 COMMON 씨앗으로 대체되어 심어진다")
    void plantRandomSeedFallbackToCommonSuccess() {
        // given
        doReturn(Grade.RARE).when(potPlantService).decideNextPlantGrade();

        // when
        PotPlantResponse response = potPlantService.plant(
                testUser.getId(),
                testPot.getId(),
                new PotPlantRequest(PlantingType.RANDOM_SEED, null)
        );

        // then
        assertThat(response.rarity()).isEqualTo("common");
        assertThat(response.growthStage()).isEqualTo(GrowthStage.SEED);
        assertThat(response.growthExp()).isEqualTo(0);
        assertThat(plantItemRepository.findActivePlantItemsByPotId(testPot.getId())).hasSize(1);
    }
}
