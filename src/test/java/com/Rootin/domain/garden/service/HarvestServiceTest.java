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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

/**
 * 식물 수확 비즈니스 로직(HarvestService)에 대한 스프링 통합 테스트 클래스입니다.
 */
@IntegrationTest
@Transactional
class HarvestServiceTest {

    @SpyBean // Mockito Spy를 사용하여 일부 메소드 동작(난수 등급 선택)을 제어하기 위해 주입합니다.
    private HarvestService harvestService;

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
    private Plant testPlantSeed;

    @BeforeEach
    void setUp() {
        // 암묵적 커밋을 유발하는 ALTER TABLE AUTO_INCREMENT DDL 구문을 제거했습니다.
        // 테스트 생성 데이터의 ID를 직접 하드코딩하지 않고 testUser.getId() 형태로 바인딩하므로 
        // 롤백 무결성을 완벽하게 보존하면서 격리를 보호합니다.

        // 1. 테스트 진행을 위한 사용자 데이터를 생성 및 저장합니다.
        testUser = User.builder()
                .email("yunseok@test.com")
                .nickname("윤석")
                .build();
        userRepository.save(testUser);

        otherUser = User.builder()
                .email("other@test.com")
                .nickname("다른사람")
                .build();
        userRepository.save(otherUser);

        // 2. 테스트 진행을 위한 화분 데이터를 생성 및 저장합니다.
        testPot = Pot.builder()
                .userId(testUser.getId())
                .title("테스트용 화분")
                .description("식물이 자라는 화분 설명")
                .level(3)
                .totalExp(350)
                .build();
        potRepository.save(testPot);

        // 3. 수확 완료 후 새로운 씨앗으로 배정될 식물 마스터 데이터(SEED 단계)를 저장합니다.
        testPlantSeed = Plant.builder()
                .name("일반 씨앗")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("common_seed_image_url")
                .silhouetteUrl("common_seed_silhouette_url")
                .build();
        plantRepository.save(testPlantSeed);
    }

    @Test
    @DisplayName("만개한 식물(경험치 1000)을 수확하면 수확 처리가 성공하여 뱃지 정보(수확레벨, 날짜 등)가 영구히 남고 새로운 씨앗이 심어진다")
    void harvestFullyGrownPlantSuccess() {
        // given
        // 경험치 1000을 달성하여 수확 가능한 활성 식물을 저장합니다.
        PlantItem activePlant = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(1000)
                .isHarvested(false)
                .build();
        plantItemRepository.save(activePlant);

        // when
        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        // then
        // 1. 반환된 API 응답 뱃지 정보 검증
        assertThat(response).isNotNull();
        assertThat(response.harvestedPlantName()).isEqualTo("일반 씨앗");
        assertThat(response.harvestedLevel()).isEqualTo(3); // 화분의 현재 레벨인 3이 찍히는지 확인

        // 2. 기존 식물 아이템이 수확된 상태(isHarvested = true)로 업데이트되었는지 및 이력 저장 검증
        PlantItem harvestedItem = plantItemRepository.findById(activePlant.getId()).orElseThrow();
        assertThat(harvestedItem.getIsHarvested()).isTrue();
        assertThat(harvestedItem.getHarvestedLevel()).isEqualTo(3);
        assertThat(harvestedItem.getHarvestedAt()).isNotNull();

        // 3. 수확 즉시 화분에 새로운 씨앗(경험치 0, SEED 단계)이 생성되어 심어졌는지 검증
        PlantItem newPlantItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(testPot.getId()).orElseThrow();
        assertThat(newPlantItem.getUserId()).isEqualTo(testUser.getId());
        assertThat(newPlantItem.getGrowthExp()).isEqualTo(0);
        assertThat(newPlantItem.getIsHarvested()).isFalse();
        assertThat(newPlantItem.getPlantId()).isEqualTo(testPlantSeed.getId());
    }

    @Test
    @DisplayName("식물의 경험치가 만개(1000)에 도달하지 못한 경우, 수확 시 BAD_REQUEST 예외가 발생한다 (수확 직후 재수확 불가 상태 검증)")
    void harvestPlantExpNotEnoughFail() {
        // given
        // 미달 경험치(500)를 가진 식물 아이템을 저장합니다.
        PlantItem activePlant = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(500)
                .isHarvested(false)
                .build();
        plantItemRepository.save(activePlant);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                harvestService.harvest(testUser.getId(), testPot.getId())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("아직 수확할 수 없습니다. 식물이 만개(경험치 1000) 단계에 도달해야 합니다.");
    }

    @Test
    @DisplayName("자신의 화분이 아닌 다른 사용자의 화분을 수확하려고 요청하면 FORBIDDEN 예외가 발생한다")
    void harvestForbiddenWhenOwnerMismatch() {
        // given
        // 수확 가능한 식물(경험치 1000) 생성
        PlantItem activePlant = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(1000)
                .isHarvested(false)
                .build();
        plantItemRepository.save(activePlant);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                harvestService.harvest(otherUser.getId(), testPot.getId()) // 타인이 수확 시도
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getMessage()).contains("해당 화분에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 화분 ID로 수확을 요청하면 NOT_FOUND 예외가 발생한다")
    void harvestNotFoundWhenPotIdNotExist() {
        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                harvestService.harvest(testUser.getId(), 9999L) // 존재하지 않는 화분 ID
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).contains("존재하지 않는 화분입니다.");
    }

    @Test
    @DisplayName("RARE 등급의 SEED 식물 마스터 데이터가 존재하지 않는 상황에서도 예외가 나지 않고, COMMON 식물로 대체(Fallback)되어 수확이 성공한다")
    void harvestRareFallbackToCommonSuccess() {
        // given
        // Spy로 주입된 서비스의 등급 결정을 스터빙하여 10%의 무작위 난수와 무관하게 100% 확정적으로 RARE 등급 분기를 타도록 합니다.
        doReturn(Grade.RARE).when(harvestService).decideNextPlantGrade();

        // RARE 등급의 식물 마스터 데이터는 전혀 없는 상태에서 만개 식물 생성
        PlantItem activePlant = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlantSeed.getId())
                .growthExp(1000)
                .isHarvested(false)
                .build();
        plantItemRepository.save(activePlant);

        // when
        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        // then
        // RARE 식물이 없는 상황이라도 에러 없이 COMMON인 '일반 씨앗'으로 Fallback되어 정상 배정됨을 검증합니다.
        assertThat(response.nextPlantName()).isEqualTo("일반 씨앗");
        
        PlantItem newPlantItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(testPot.getId()).orElseThrow();
        assertThat(newPlantItem.getPlantId()).isEqualTo(testPlantSeed.getId());
    }
}
