package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.HarvestResponse;
import com.Rootin.domain.garden.entity.PlantCollection;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantCollectionRepository;
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
 * [씨앗 배정 정책 — CASE B / CASE C]
 *   FULL_BLOOM(stageIndex=4) 수확 → 전체 풀 랜덤 배정 + 새 종이면 해금 풀(plant_collection)에 추가
 *   FULL_BLOOM 미만 수확      → 기존 해금 풀 랜덤 배정 (풀 변화 없음)
 *
 * [stageIndex 기준 — LevelCalculator.determinePlantGrowthStage()]
 *   0=씨앗(0~199), 1=새싹(200~499), 2=잎(500~799), 3=개화(800~999), 4=만개(1000+)
 *
 * [SpyBean 대상 변경]
 *   기존: HarvestService.decideNextPlantGrade()
 *   변경: SeedAssignmentService.decideGrade() — 씨앗 배정 확률 로직이 SeedAssignmentService로 이동함
 */
@IntegrationTest
@Transactional
class HarvestServiceTest {

    @SpyBean
    private SeedAssignmentService seedAssignmentService;

    @Autowired private HarvestService harvestService;
    @Autowired private UserRepository userRepository;
    @Autowired private PotRepository potRepository;
    @Autowired private PlantItemRepository plantItemRepository;
    @Autowired private PlantRepository plantRepository;
    @Autowired private PlantCollectionRepository plantCollectionRepository;

    private User testUser;
    private User otherUser;
    private Pot testPot;
    private Plant testPlantSeed;

    /**
     * 각 테스트 실행 전 공통 픽스처를 생성합니다.
     * - testUser / otherUser: 소유권 검증 시나리오용 두 사용자
     * - testPot: level=3 화분 (응답 DTO의 harvestedLevel=3 검증 기준)
     * - testPlantSeed: DB에 저장되는 유일한 COMMON SEED 식물
     * - PlantCollection: FULL_BLOOM 미만 수확 시 해금 풀에서 씨앗 배정 → testUser의 해금 풀 초기화
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
        plantCollectionRepository.save(PlantCollection.builder()
                .userId(testUser.getId()).plantId(testPlantSeed.getId()).build());
    }

    /**
     * [만개 단계 수확] growthExp=1000 → stageIndex=4
     * 수확 응답 DTO(plantName·harvestedLevel), 수확된 PlantItem의 상태(isHarvested·stageIndex·harvestedAt),
     * 화분에 새 씨앗(growthExp=0)이 자동 배정되는지를 종합 검증합니다.
     */
    @Test
    @DisplayName("만개 단계(경험치 1000)에서 수확하면 harvestedStageIndex=4가 기록된다")
    void harvest_fullBloom_stageIndex4() {
        PlantItem activePlant = plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response).isNotNull();
        assertThat(response.harvestedPlantName()).isEqualTo("일반 씨앗");
        assertThat(response.harvestedLevel()).isEqualTo(3);
        assertThat(response.harvestedStageIndex()).isEqualTo(4);

        PlantItem harvested = plantItemRepository.findById(activePlant.getId()).orElseThrow();
        assertThat(harvested.getIsHarvested()).isTrue();
        assertThat(harvested.getHarvestedStageIndex()).isEqualTo(4);
        assertThat(harvested.getHarvestedAt()).isNotNull();

        PlantItem newItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(testPot.getId()).orElseThrow();
        assertThat(newItem.getGrowthExp()).isEqualTo(0);
        assertThat(newItem.getIsHarvested()).isFalse();
    }

    /**
     * [개화 단계 수확] growthExp=850 → stageIndex=3 (FULL_BLOOM 미만 → 해금 풀 랜덤 배정)
     */
    @Test
    @DisplayName("개화 단계(경험치 800~999)에서 수확하면 harvestedStageIndex=3이 기록된다")
    void harvest_bloom_stageIndex3() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(850).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response.harvestedStageIndex()).isEqualTo(3);
        PlantItem harvestedItem = plantItemRepository
                .findByUserIdAndIsHarvestedTrue(testUser.getId()).stream()
                .findFirst().orElseThrow();
        assertThat(harvestedItem.getHarvestedStageIndex()).isEqualTo(3);
    }

    /**
     * [새싹 단계 수확] growthExp=300 → stageIndex=1 (FULL_BLOOM 미만 → 해금 풀 랜덤 배정)
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
     * [씨앗 단계 수확] growthExp=50 → stageIndex=0 (FULL_BLOOM 미만 → 해금 풀 랜덤 배정)
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
     * [만개 수확 → 새 종 해금] RARE 씨앗을 강제 배정(Spy) → 해금 풀에 추가되는지 검증.
     * decideGrade()를 RARE 반환으로 고정하여 신규 RARE 씨앗이 collection에 insert되는 흐름을 확인합니다.
     */
    @Test
    @DisplayName("만개 수확 시 처음 등장한 씨앗 종은 해금 풀(plant_collection)에 추가된다")
    void harvest_fullBloom_addsNewSeedToCollection() {
        Plant rareSeed = plantRepository.save(Plant.builder()
                .name("달빛씨앗").grade(Grade.RARE).growthStage(GrowthStage.SEED)
                .imageUrl("rare_url").silhouetteUrl("rare_sil").build());
        doReturn(Grade.RARE).when(seedAssignmentService).decideGrade();

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        int beforeCount = plantCollectionRepository.findPlantIdsByUserId(testUser.getId()).size();
        harvestService.harvest(testUser.getId(), testPot.getId());
        int afterCount = plantCollectionRepository.findPlantIdsByUserId(testUser.getId()).size();

        assertThat(afterCount).isEqualTo(beforeCount + 1);
        assertThat(plantCollectionRepository.existsByUserIdAndPlantId(testUser.getId(), rareSeed.getId())).isTrue();
    }

    /**
     * [만개 미만 수확 → 해금 풀 변화 없음] FULL_BLOOM 미만 수확 시 collection insert가 발생하지 않는지 검증.
     */
    @Test
    @DisplayName("만개 미만 수확 시 해금 풀(plant_collection)에 새 씨앗이 추가되지 않는다")
    void harvest_belowFullBloom_doesNotExpandCollection() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(300).isHarvested(false).build());

        int beforeCount = plantCollectionRepository.findPlantIdsByUserId(testUser.getId()).size();
        harvestService.harvest(testUser.getId(), testPot.getId());
        int afterCount = plantCollectionRepository.findPlantIdsByUserId(testUser.getId()).size();

        assertThat(afterCount).isEqualTo(beforeCount);
    }

    /**
     * [소유권 검증] otherUser가 testUser 화분을 수확하려 하면 FORBIDDEN.
     */
    @Test
    @DisplayName("다른 사용자의 화분을 수확하려 하면 FORBIDDEN 예외가 발생한다")
    void harvest_forbidden_ownerMismatch() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        CustomException ex = assertThrows(CustomException.class, () ->
                harvestService.harvest(otherUser.getId(), testPot.getId()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * [존재하지 않는 화분] potId=9999로 수확 요청 시 NOT_FOUND.
     */
    @Test
    @DisplayName("존재하지 않는 화분 ID로 수확을 요청하면 NOT_FOUND 예외가 발생한다")
    void harvest_notFound_potNotExist() {
        CustomException ex = assertThrows(CustomException.class, () ->
                harvestService.harvest(testUser.getId(), 9999L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * [RARE Fallback] decideGrade()를 Spy로 RARE 반환 강제 → DB에 RARE 없으면 COMMON 대체.
     * FULL_BLOOM 수확(전체 풀 랜덤)에서 RARE가 없어도 COMMON으로 Fallback되어 수확이 정상 완료되는지 검증합니다.
     */
    @Test
    @DisplayName("RARE 등급 식물이 DB에 없으면 COMMON으로 Fallback되어 수확이 성공한다")
    void harvest_rareFallbackToCommon() {
        doReturn(Grade.RARE).when(seedAssignmentService).decideGrade();

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(testPlantSeed.getId())
                .growthExp(1000).isHarvested(false).build());

        HarvestResponse response = harvestService.harvest(testUser.getId(), testPot.getId());

        assertThat(response.nextPlantName()).isEqualTo("일반 씨앗");
    }
}
