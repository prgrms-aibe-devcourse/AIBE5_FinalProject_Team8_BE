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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

/**
 * HarvestService 통합 테스트.
 *
 * [변경 배경]
 * - 기존: 경험치 1000(만개) 이상이어야만 수확 가능 → FULL_BLOOM 제약 존재
 * - 변경: 어느 성장 단계에서든 수확 허용, 수확 시점의 stageIndex를 PlantItem.harvestedStageIndex에 기록
 * - 삭제된 테스트: "exp < 1000이면 BAD_REQUEST" (제약 제거로 더 이상 유효하지 않음)
 * - 추가된 테스트: 각 단계(씨앗/새싹/개화/만개)별로 올바른 stageIndex가 기록되는지 검증
 *
 * [stageIndex 기준 — LevelCalculator.determinePlantGrowthStage()]
 *   0=씨앗(0~199), 1=새싹(200~499), 2=잎(500~799), 3=개화(800~999), 4=만개(1000+)
 */
@IntegrationTest
@Transactional
class HarvestServiceTest {

    // HarvestService.decideNextPlantGrade()를 Spy로 감싸서
    // 특정 테스트에서 RARE 등급을 강제 반환해 Fallback 분기를 검증합니다.
    @SpyBean
    private HarvestService harvestService;

    @Autowired private UserRepository userRepository;
    @Autowired private PotRepository potRepository;
    @Autowired private PlantItemRepository plantItemRepository;
    @Autowired private PlantRepository plantRepository;

    private User testUser;   // 화분 소유자
    private User otherUser;  // 소유권 불일치 시나리오용 타 사용자
    private Pot testPot;     // 공통 테스트 화분 (level=3 → harvestedLevel 검증에 사용)
    private Plant testPlantSeed; // COMMON/SEED 식물 마스터 (다음 씨앗 Fallback에도 재사용)

    /**
     * 각 테스트 실행 전 공통 픽스처를 생성합니다.
     * - testUser / otherUser: 소유권 검증 시나리오용 두 사용자
     * - testPot: level=3 화분 (응답 DTO의 harvestedLevel=3 검증 기준)
     * - testPlantSeed: DB에 저장되는 유일한 COMMON SEED 식물 → RARE Fallback 시 대체 대상
     */
    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("yunseok@test.com").nickname("윤석").build());
        otherUser = userRepository.save(User.builder()
                .email("other@test.com").nickname("다른사람").build());
        testPot = potRepository.save(Pot.builder()
                .userId(testUser.getId()).title("테스트용 화분").description("설명")
                .level(3).totalExp(350).build());
        testPlantSeed = plantRepository.save(Plant.builder()
                .name("일반 씨앗").grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                .imageUrl("common_seed_image_url").silhouetteUrl("common_seed_silhouette_url").build());
    }

    /**
     * [만개 단계 수확] growthExp=1000 → stageIndex=4
     * 수확 응답 DTO(plantName·harvestedLevel), 수확된 PlantItem의 상태(isHarvested·stageIndex·harvestedAt),
     * 화분에 새 씨앗(growthExp=0)이 자동 배정되는지를 종합 검증합니다.
     */
    @Test
    @DisplayName("만개 단계(경험치 1000)에서 수확하면 harvestedStageIndex=4가 기록된다")
    void harvest_fullBloom_stageIndex4() {
        // Arrange: 만개 수준 경험치를 가진 활성 식물 저장
        PlantItem activePlant = plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        // Act
        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        // Assert — 응답 DTO 검증
        assertThat(response).isNotNull();
        assertThat(response.harvestedPlantName()).isEqualTo("일반 씨앗");
        assertThat(response.harvestedLevel()).isEqualTo(3); // testPot.level
        assertThat(response.harvestedStageIndex()).isEqualTo(4);

        // Assert — 수확된 PlantItem 상태 검증
        PlantItem harvested = plantItemRepository.findById(activePlant.getId()).orElseThrow();
        assertThat(harvested.getIsHarvested()).isTrue();
        assertThat(harvested.getHarvestedStageIndex()).isEqualTo(4);
        assertThat(harvested.getHarvestedAt()).isNotNull();

        // Assert — 화분에 새 씨앗이 자동으로 심어졌는지 확인
        PlantItem newItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(testPot.getId()).orElseThrow();
        assertThat(newItem.getGrowthExp()).isEqualTo(0);
        assertThat(newItem.getIsHarvested()).isFalse();
    }

    /**
     * [개화 단계 수확] growthExp=850 → stageIndex=3
     * FULL_BLOOM 제약이 제거되어 개화 단계에서도 수확이 허용되는지,
     * harvestedStageIndex=3이 기록되어 도감의 개화 슬롯(index 3)을 해금할 수 있는지 검증합니다.
     */
    @Test
    @DisplayName("개화 단계(경험치 800~999)에서 수확하면 harvestedStageIndex=3이 기록된다")
    void harvest_bloom_stageIndex3() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(850).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response.harvestedStageIndex()).isEqualTo(3);
        // 수확 후 findByPotIdAndIsHarvestedFalse는 새로 심어진 씨앗을 반환하므로
        // 수확된 항목은 findByUserIdAndIsHarvestedTrue로 별도 조회합니다.
        PlantItem harvestedItem = plantItemRepository
                .findByUserIdAndIsHarvestedTrue(testUser.getId()).stream()
                .findFirst().orElseThrow();
        assertThat(harvestedItem.getHarvestedStageIndex()).isEqualTo(3);
    }

    /**
     * [새싹 단계 수확] growthExp=300 → stageIndex=1
     * 아직 초기 단계인 새싹에서도 수확이 가능하고,
     * harvestedStageIndex=1이 기록되어 도감 새싹 슬롯을 해금하는 흐름을 지원하는지 확인합니다.
     */
    @Test
    @DisplayName("새싹 단계(경험치 200~499)에서 수확하면 harvestedStageIndex=1이 기록된다")
    void harvest_sprout_stageIndex1() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(300).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response.harvestedStageIndex()).isEqualTo(1);
        PlantItem harvestedItem = plantItemRepository
                .findByUserIdAndIsHarvestedTrue(testUser.getId()).stream()
                .findFirst().orElseThrow();
        assertThat(harvestedItem.getHarvestedStageIndex()).isEqualTo(1);
    }

    /**
     * [씨앗 단계 수확] growthExp=50 → stageIndex=0
     * 경험치가 거의 없는 씨앗 상태에서도 수확이 허용되고,
     * harvestedStageIndex=0이 기록되어 도감 씨앗 슬롯(index 0)을 해금할 수 있는지 검증합니다.
     */
    @Test
    @DisplayName("씨앗 단계(경험치 0~199)에서 수확하면 harvestedStageIndex=0이 기록된다")
    void harvest_seed_stageIndex0() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(50).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response.harvestedStageIndex()).isEqualTo(0);
        PlantItem harvestedItem = plantItemRepository
                .findByUserIdAndIsHarvestedTrue(testUser.getId()).stream()
                .findFirst().orElseThrow();
        assertThat(harvestedItem.getHarvestedStageIndex()).isEqualTo(0);
    }

    /**
     * [소유권 검증] otherUser가 testUser 화분을 수확하려 하면 FORBIDDEN.
     * 화분의 userId와 요청 userId가 일치하지 않을 때 접근을 차단하는지 확인합니다.
     */
    @Test
    @DisplayName("다른 사용자의 화분을 수확하려 하면 FORBIDDEN 예외가 발생한다")
    void harvest_forbidden_ownerMismatch() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        // otherUser가 testUser 화분에 수확 요청 → FORBIDDEN
        CustomException ex = assertThrows(CustomException.class, () ->
                harvestService.harvest(otherUser.getId(), testPot.getId()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * [존재하지 않는 화분] potId=9999로 수확 요청 시 NOT_FOUND.
     * 화분 조회 실패 시 CustomException이 적절한 HTTP 상태와 함께 발생하는지 확인합니다.
     */
    @Test
    @DisplayName("존재하지 않는 화분 ID로 수확을 요청하면 NOT_FOUND 예외가 발생한다")
    void harvest_notFound_potNotExist() {
        CustomException ex = assertThrows(CustomException.class, () ->
                harvestService.harvest(testUser.getId(), 9999L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * [RARE Fallback] decideNextPlantGrade()를 Spy로 RARE 반환 강제 → DB에 RARE 없으면 COMMON 대체.
     * 수확 후 다음 씨앗 선택 시 RARE 등급이 DB에 없어도 COMMON으로 Fallback되어
     * 수확이 정상 완료되는지 검증합니다. (운영 초기 RARE 데이터 미등록 상황 대비)
     */
    @Test
    @DisplayName("RARE 등급 식물이 DB에 없으면 COMMON으로 Fallback되어 수확이 성공한다")
    void harvest_rareFallbackToCommon() {
        // decideNextPlantGrade()가 항상 RARE를 반환하도록 Spy 설정
        doReturn(Grade.RARE).when(harvestService).decideNextPlantGrade();

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        // RARE 식물이 없으므로 setUp()에서 저장한 COMMON "일반 씨앗"으로 Fallback되어 배정됨
        assertThat(response.nextPlantName()).isEqualTo("일반 씨앗");
    }
}
