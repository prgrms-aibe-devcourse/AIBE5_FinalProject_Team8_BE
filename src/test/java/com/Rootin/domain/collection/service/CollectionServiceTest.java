package com.Rootin.domain.collection.service;

import com.Rootin.domain.collection.dto.CollectionDexResponse;
import com.Rootin.domain.collection.dto.DexEntry;
import com.Rootin.domain.collection.dto.DexSection;
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
 * CollectionService 통합 테스트.
 *
 * [변경 배경]
 * - 기존 로직: 수확 횟수(harvestCount)를 순서대로 채워 stage 0→1→2→... 해금 (순차 방식)
 * - 변경 로직: PlantItem.harvestedStageIndex 필드를 직접 참조해 해당 슬롯만 점등 (stageIndex 방식)
 *   → 씨앗 상태에서 수확하면 stageIndex=0 슬롯만 켜지고, 나머지는 잠금 유지
 * - 폴백 정책: harvestedStageIndex=null인 기존 데이터는 4(만개)로 간주 (마이그레이션 전 데이터 보호)
 *
 * [도감 고정 구조]
 *   8종 × 5단계 = 40칸, COMMON 5종(25칸) / RARE 3종(15칸)
 */
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
    private Plant seedPlant;  // COMMON 종 "기본 씨앗" → speciesKey="seed"
    private Plant moonPlant;  // RARE 종 "달빛씨앗" → speciesKey="moon"

    /**
     * 각 테스트 실행 전 공통 픽스처를 생성합니다.
     * - seedPlant: COMMON 대표 종 (대부분의 슬롯 점등 테스트에 사용)
     * - moonPlant: RARE 종 (rare=true 섹션 분류 및 희귀종 수확 검증에 사용)
     */
    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com").nickname("도감사용자").build());
        testPot = potRepository.save(Pot.builder()
                .userId(testUser.getId()).title("자바 정복 화분")
                .level(4).totalExp(600).build());
        seedPlant = plantRepository.save(Plant.builder()
                .name("기본 씨앗").grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                .imageUrl("seed_url").build());
        moonPlant = plantRepository.save(Plant.builder()
                .name("달빛씨앗").grade(Grade.RARE).growthStage(GrowthStage.SEED)
                .imageUrl("moon_url").build());
    }

    /**
     * [고정 구조 검증] 수확 이력 없이도 도감 응답은 40칸·8섹션·통계를 반환해야 합니다.
     * 도감은 실제 수확 여부와 관계없이 항상 고정된 뼈대 구조를 반환하는 것이 전제 조건입니다.
     */
    @Test
    @DisplayName("도감 응답은 40칸 고정 구조(8종 × 5단계)와 통계를 반환한다")
    void getDex_returnsFixedStructure() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        assertThat(response.stats().total()).isEqualTo(40);
        assertThat(response.stats().common()).isEqualTo(25);   // COMMON 5종 × 5단계
        assertThat(response.stats().rare()).isEqualTo(15);     // RARE 3종 × 5단계
        assertThat(response.stats().collected()).isZero();     // 수확 없음 → 해금 0
        assertThat(response.sections()).hasSize(8);
        assertThat(response.sections().stream().mapToLong(s -> s.entries().size()).sum()).isEqualTo(40);
    }

    /**
     * [stageIndex 슬롯 점등] stageIndex=0과 stageIndex=2에서 수확 → 해당 슬롯만 collected=true.
     * 수확한 단계와 다른 슬롯(1·3·4)은 점등되지 않아야 합니다.
     * 이것이 기존 순차 방식과의 핵심 차이점입니다.
     */
    @Test
    @DisplayName("수확한 stageIndex에 해당하는 슬롯만 점등된다 — 씨앗(0)·잎(2) 수확 시 해당 슬롯만 collected=true")
    void harvest_specificStageIndex_lightsUpOnlyThatSlot() {
        // Arrange: seed 종을 stage 0(씨앗)과 stage 2(잎)에서 각각 수확
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(50).isHarvested(true).harvestedLevel(1)
                .harvestedStageIndex(0)
                .harvestedAt(LocalDateTime.of(2026, 1, 10, 10, 0)).build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(600).isHarvested(true).harvestedLevel(3)
                .harvestedStageIndex(2)
                .harvestedAt(LocalDateTime.of(2026, 3, 5, 9, 0)).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        List<DexEntry> entries = seedSection(response).entries();

        assertThat(entries.get(0).collected()).isTrue();   // stage 0 점등
        assertThat(entries.get(1).collected()).isFalse();  // stage 1 미수집 (수확 안 함)
        assertThat(entries.get(2).collected()).isTrue();   // stage 2 점등
        assertThat(entries.get(3).collected()).isFalse();  // stage 3 미수집
        assertThat(entries.get(4).collected()).isFalse();  // stage 4 미수집

        assertThat(response.stats().collected()).isEqualTo(2);
    }

    /**
     * [중복 수확 슬롯 처리] 동일 stageIndex를 여러 번 수확해도 슬롯은 1개만 점등되고,
     * harvestedAt은 earliest date(최초 수확 날짜)가 표시됩니다.
     * CollectionService는 Map.merge()로 최솟값을 유지합니다.
     */
    @Test
    @DisplayName("같은 stageIndex를 여러 번 수확해도 슬롯은 하나만 점등되고, 최초 수확 날짜가 기록된다")
    void harvest_sameStageMultipleTimes_onlyOneSlotWithEarliestDate() {
        LocalDateTime earlier = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime later   = LocalDateTime.of(2026, 4, 1, 10, 0);

        // Arrange: stage 4(만개)를 두 번 수확 (earlier → later 순서)
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(5)
                .harvestedStageIndex(4).harvestedAt(earlier).build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(7)
                .harvestedStageIndex(4).harvestedAt(later).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        List<DexEntry> entries = seedSection(response).entries();

        assertThat(entries.get(4).collected()).isTrue();
        assertThat(entries.get(4).harvestedAt()).isEqualTo("2026.02.01"); // earlier 날짜가 유지됨
        assertThat(response.stats().collected()).isEqualTo(1);            // 슬롯은 1개만 점등
    }

    /**
     * [null stageIndex 폴백] harvestedStageIndex=null인 기존 수확 데이터는 stage 4(만개)로 처리.
     * harvestedStageIndex 컬럼이 추가되기 전 저장된 레거시 데이터를 올바르게 처리합니다.
     */
    @Test
    @DisplayName("harvestedStageIndex가 null인 기존 데이터는 stage 4(만개)로 폴백 처리된다")
    void harvest_nullStageIndex_fallbackToFullBloom() {
        // Arrange: harvestedStageIndex를 빌더에서 설정하지 않으면 null로 저장됨
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(5)
                .harvestedAt(LocalDateTime.of(2026, 5, 1, 12, 0)).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        List<DexEntry> entries = seedSection(response).entries();

        assertThat(entries.get(4).collected()).isTrue();   // null → stage 4로 폴백
        assertThat(entries.get(0).collected()).isFalse();  // 다른 슬롯은 영향 없음
        assertThat(response.stats().collected()).isEqualTo(1);
    }

    /**
     * [수확 없음] 수확 이력이 전혀 없으면 모든 슬롯이 잠금(collected=false, monName=null)이어야 합니다.
     */
    @Test
    @DisplayName("수확이 없는 species는 모든 단계가 collected=false다")
    void noHarvest_allLocked() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        DexSection seed = seedSection(response);

        assertThat(seed.entries()).allMatch(e -> !e.collected());
        assertThat(seed.entries()).allMatch(e -> e.monName() == null); // 잠금 상태엔 이름 없음
    }

    /**
     * [희귀종 분류] RARE 등급의 달빛씨앗(speciesKey="moon")을 수확하면
     * 섹션의 rare=true 플래그와 해당 단계 슬롯 점등, 몬스터 이름이 올바르게 반환됩니다.
     */
    @Test
    @DisplayName("희귀 종(달빛씨앗)은 rare=true 섹션으로 분류되고 수확 시 해당 슬롯이 점등된다")
    void rareSpecies_markedRareAndCollected() {
        // Arrange: 달빛씨앗을 stage 4(만개)에서 수확
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(moonPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(3)
                .harvestedStageIndex(4)
                .harvestedAt(LocalDateTime.of(2026, 6, 1, 12, 0)).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        DexSection moonSection = response.sections().stream()
                .filter(s -> s.speciesKey().equals("moon")).findFirst().orElseThrow();

        assertThat(moonSection.rare()).isTrue();                             // RARE 분류 확인
        assertThat(moonSection.entries().get(4).collected()).isTrue();       // stage 4 점등
        assertThat(moonSection.entries().get(4).monName()).isEqualTo("달빛왕"); // 만개 단계 이름
        assertThat(moonSection.entries().get(0).collected()).isFalse();      // 미수확 슬롯은 잠금
    }

    /**
     * [도감 번호 순서] sections 순서대로 001~040이 연속 부여되는지 전수 확인합니다.
     * 섹션 순서나 엔트리 순서가 바뀌면 도감 번호 연속성이 깨지므로 회귀 방지용으로 검증합니다.
     */
    @Test
    @DisplayName("도감 번호는 001~040으로 sections 순서대로 연속 부여된다")
    void dexNumbers_sequential() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());
        int expected = 1;
        for (DexSection section : response.sections()) {
            for (DexEntry entry : section.entries()) {
                assertThat(entry.dexNumber()).isEqualTo(String.format("%03d", expected++));
            }
        }
        assertThat(expected).isEqualTo(41); // 마지막 번호 40 이후 41로 증가했는지 확인
    }

    /**
     * [알 수 없는 식물 무시] PLANT_NAME_TO_SPECIES 맵에 없는 식물명을 가진 수확 이력은 도감에 반영되지 않습니다.
     * 신규 식물 추가 시 CollectionService의 매핑 테이블도 함께 업데이트해야 하는 이유를 보여줍니다.
     */
    @Test
    @DisplayName("매핑되지 않는 식물명의 수확은 도감에 영향을 주지 않는다")
    void unmappedPlant_ignoredInDex() {
        // Arrange: 매핑 테이블에 없는 이름의 식물을 DB에 저장 후 수확 처리
        Plant unknown = plantRepository.save(Plant.builder()
                .name("알 수 없는 식물").grade(Grade.COMMON).growthStage(GrowthStage.SEED).build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(unknown.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(2)
                .harvestedStageIndex(4).harvestedAt(LocalDateTime.now()).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        // 매핑 불가 식물이므로 collected 카운트에 포함되지 않아야 함
        assertThat(response.stats().collected()).isZero();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    // "기본 씨앗"(speciesKey="seed") 섹션을 응답에서 꺼내는 헬퍼 메서드
    private DexSection seedSection(CollectionDexResponse response) {
        return response.sections().stream()
                .filter(s -> s.speciesKey().equals("seed"))
                .findFirst().orElseThrow();
    }
}
