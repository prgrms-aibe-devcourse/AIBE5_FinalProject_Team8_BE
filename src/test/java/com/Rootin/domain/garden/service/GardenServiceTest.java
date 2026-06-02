package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.GardenLayoutUpdateRequest;
import com.Rootin.domain.garden.dto.GardenResponse;
import com.Rootin.domain.garden.dto.LayoutUpdateDto;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
@Transactional
class GardenServiceTest {

    @Autowired
    private GardenService gardenService;

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
    private PlantItem activePlantItem;
    private PlantItem harvestedPlantItem;
    private Plant testPlantSeed;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .nickname("테스터")
                .gardenTheme(GardenTheme.FOREST)
                .build();
        userRepository.save(testUser);

        otherUser = User.builder()
                .email("other@test.com")
                .nickname("타인")
                .build();
        userRepository.save(otherUser);

        testPot = Pot.builder()
                .userId(testUser.getId())
                .title("테스트 화분")
                .description("설명")
                .level(1)
                .totalExp(0)
                .isDisplayed(false)
                .build();
        potRepository.save(testPot);

        testPlantSeed = Plant.builder()
                .name("씨앗몬")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("seed_image")
                .build();
        plantRepository.save(testPlantSeed);

        // 새싹(SPROUT) 단계의 마스터 식물 이미지 메타데이터 추가
        Plant testPlantSprout = Plant.builder()
                .name("씨앗몬")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SPROUT)
                .imageUrl("sprout_image")
                .build();
        plantRepository.save(testPlantSprout);

        // 만개(FULL_BLOOM) 단계의 마스터 식물 이미지 메타데이터 추가
        Plant testPlantFullBloom = Plant.builder()
                .name("씨앗몬")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.FULL_BLOOM)
                .imageUrl("full_bloom_image")
                .build();
        plantRepository.save(testPlantFullBloom);

        // 화분에 심겨 있는 수확 전 식물 (경험치 300 -> SPROUT 단계로 설정)
        activePlantItem = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(300)
                .isHarvested(false)
                .isDisplayed(false)
                .build();
        plantItemRepository.save(activePlantItem);

        // 정원에 배치 가능한 수확 완료 식물 (경험치 1000 -> FULL_BLOOM 단계로 설정)
        harvestedPlantItem = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(1000)
                .isHarvested(true)
                .harvestedLevel(1)
                .isDisplayed(true)
                .positionX(10)
                .positionY(20)
                .build();
        plantItemRepository.save(harvestedPlantItem);
    }

    @Test
    @DisplayName("사용자의 정원 정보를 올바르게 조회한다 (활성 식물 및 수확 식물 구분, 성장 단계별 이미지 동적 할당 검증)")
    void getGardenSuccess() {
        GardenResponse response = gardenService.getGarden(testUser.getId());

        assertThat(response.getTheme()).isEqualTo(GardenTheme.FOREST);

        // 1. 화분 내 활성 식물(300 exp -> SPROUT 단계)의 이름과 이미지 동적 매핑 검증
        assertThat(response.getPots()).hasSize(1);
        assertThat(response.getPots().get(0).getId()).isEqualTo(testPot.getId());
        assertThat(response.getPots().get(0).getPlantName()).isEqualTo("씨앗몬");
        assertThat(response.getPots().get(0).getGrowthStage()).isEqualTo(GrowthStage.SPROUT);
        assertThat(response.getPots().get(0).getImageUrl()).isEqualTo("sprout_image"); // "sprout_image" 매핑 검증

        // 2. 수확한 식물(1000 exp -> FULL_BLOOM 단계)의 이미지 매핑 검증
        assertThat(response.getHarvestedPlants()).hasSize(1);
        assertThat(response.getHarvestedPlants().get(0).getId()).isEqualTo(harvestedPlantItem.getId());
        assertThat(response.getHarvestedPlants().get(0).getImageUrl()).isEqualTo("full_bloom_image"); // "full_bloom_image" 매핑 검증
        assertThat(response.getHarvestedPlants().get(0).getPositionX()).isEqualTo(10);
    }

    @Test
    @DisplayName("사용자의 정원 테마를 성공적으로 업데이트한다")
    void updateGardenThemeSuccess() {
        gardenService.updateGardenTheme(testUser.getId(), GardenTheme.NIGHT);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getGardenTheme()).isEqualTo(GardenTheme.NIGHT);
    }

    @Test
    @DisplayName("화분과 수확된 식물의 레이아웃 정보를 일괄 업데이트한다")
    void updateGardenLayoutSuccess() {
        LayoutUpdateDto potUpdate = new LayoutUpdateDto(testPot.getId(), true, 100, 200);
        LayoutUpdateDto plantUpdate = new LayoutUpdateDto(harvestedPlantItem.getId(), false, null, null);

        GardenLayoutUpdateRequest request = new GardenLayoutUpdateRequest(
                List.of(potUpdate),
                List.of(plantUpdate)
        );

        gardenService.updateGardenLayout(testUser.getId(), request);

        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getIsDisplayed()).isTrue();
        assertThat(updatedPot.getPositionX()).isEqualTo(100);
        assertThat(updatedPot.getPositionY()).isEqualTo(200);

        PlantItem updatedPlantItem = plantItemRepository.findById(harvestedPlantItem.getId()).orElseThrow();
        assertThat(updatedPlantItem.getIsDisplayed()).isFalse();
        assertThat(updatedPlantItem.getPositionX()).isNull(); // isDisplayed = false이므로 null 강제 보정 검증
        assertThat(updatedPlantItem.getPositionY()).isNull();
    }

    @Test
    @DisplayName("isDisplayed가 true인데 좌표가 null이거나 음수이면 Bad Request 예외가 발생한다")
    void updateGardenLayoutFailWhenCoordinatesInvalid() {
        // positionX가 null
        LayoutUpdateDto invalidPotUpdate = new LayoutUpdateDto(testPot.getId(), true, null, 200);
        GardenLayoutUpdateRequest request1 = new GardenLayoutUpdateRequest(List.of(invalidPotUpdate), List.of());

        CustomException exception1 = assertThrows(CustomException.class, () ->
                gardenService.updateGardenLayout(testUser.getId(), request1)
        );
        assertThat(exception1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        // positionY가 음수
        LayoutUpdateDto invalidPlantUpdate = new LayoutUpdateDto(harvestedPlantItem.getId(), true, 10, -5);
        GardenLayoutUpdateRequest request2 = new GardenLayoutUpdateRequest(List.of(), List.of(invalidPlantUpdate));

        CustomException exception2 = assertThrows(CustomException.class, () ->
                gardenService.updateGardenLayout(testUser.getId(), request2)
        );
        assertThat(exception2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("타인 소유의 화분을 업데이트하려 하면 Forbidden 예외가 발생한다")
    void updateGardenLayoutFailWhenNotOwner() {
        LayoutUpdateDto potUpdate = new LayoutUpdateDto(testPot.getId(), true, 10, 10);
        GardenLayoutUpdateRequest request = new GardenLayoutUpdateRequest(List.of(potUpdate), List.of());

        CustomException exception = assertThrows(CustomException.class, () ->
                gardenService.updateGardenLayout(otherUser.getId(), request) // 타인이 수정 시도
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("수확되지 않은 식물을 정원 캔버스에 개별 배치하려 하면 Bad Request 예외가 발생한다")
    void updateGardenLayoutFailWhenPlantNotHarvested() {
        // activePlantItem은 수확되지 않음 (isHarvested=false)
        LayoutUpdateDto plantUpdate = new LayoutUpdateDto(activePlantItem.getId(), true, 10, 10);
        GardenLayoutUpdateRequest request = new GardenLayoutUpdateRequest(List.of(), List.of(plantUpdate));

        CustomException exception = assertThrows(CustomException.class, () ->
                gardenService.updateGardenLayout(testUser.getId(), request)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("수확되지 않은 식물");
    }
}
