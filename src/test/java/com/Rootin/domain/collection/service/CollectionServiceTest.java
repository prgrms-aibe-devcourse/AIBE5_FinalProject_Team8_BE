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
    private Plant seedPlant;
    private Plant moonPlant;

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

        seedPlant = Plant.builder()
                .name("기본 씨앗").grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                .imageUrl("seed_url").build();
        plantRepository.save(seedPlant);

        moonPlant = Plant.builder()
                .name("달빛씨앗").grade(Grade.RARE).growthStage(GrowthStage.SEED)
                .imageUrl("moon_url").build();
        plantRepository.save(moonPlant);
    }

    @Test
    @DisplayName("도감 응답은 40칸 고정 구조(8종 × 5단계)와 통계를 반환한다")
    void getDex_returnsFixedStructure() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        assertThat(response.stats().total()).isEqualTo(40);
        assertThat(response.stats().common()).isEqualTo(25);
        assertThat(response.stats().rare()).isEqualTo(15);
        assertThat(response.stats().collected()).isZero();
        assertThat(response.sections()).hasSize(8);

        long totalEntries = response.sections().stream()
                .mapToLong(s -> s.entries().size())
                .sum();
        assertThat(totalEntries).isEqualTo(40);
    }

    @Test
    @DisplayName("N번 수확하면 해당 species의 stage 0~N-1이 collected=true로 표시된다")
    void harvest_unlocksStagesInOrder() {
        LocalDateTime first  = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime second = LocalDateTime.of(2026, 6, 1, 15, 0);
        LocalDateTime third  = LocalDateTime.of(2026, 6, 5, 9, 0);

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(4).harvestedAt(first).build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(5).harvestedAt(second).build());
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(seedPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(6).harvestedAt(third).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        DexSection seedSection = response.sections().stream()
                .filter(s -> s.speciesKey().equals("seed"))
                .findFirst().orElseThrow();

        List<DexEntry> entries = seedSection.entries();
        assertThat(entries.get(0).collected()).isTrue();
        assertThat(entries.get(0).monName()).isEqualTo("씨드몬");
        assertThat(entries.get(0).harvestedAt()).isEqualTo("2026.05.20");
        assertThat(entries.get(1).collected()).isTrue();
        assertThat(entries.get(1).monName()).isEqualTo("새싹몬");
        assertThat(entries.get(1).harvestedAt()).isEqualTo("2026.06.01");
        assertThat(entries.get(2).collected()).isTrue();
        assertThat(entries.get(2).monName()).isEqualTo("잎몬");
        assertThat(entries.get(3).collected()).isFalse();
        assertThat(entries.get(3).monName()).isNull();
        assertThat(entries.get(4).collected()).isFalse();

        assertThat(response.stats().collected()).isEqualTo(3);
    }

    @Test
    @DisplayName("수확이 없는 species는 모든 단계가 locked(collected=false)로 표시된다")
    void noHarvest_allLocked() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        DexSection seedSection = response.sections().stream()
                .filter(s -> s.speciesKey().equals("seed"))
                .findFirst().orElseThrow();

        assertThat(seedSection.entries()).allMatch(e -> !e.collected());
        assertThat(seedSection.entries()).allMatch(e -> e.monName() == null);
        assertThat(seedSection.rare()).isFalse();
    }

    @Test
    @DisplayName("희귀 종(RARE) species는 rare=true 섹션으로 분류된다")
    void rareSpecies_markedRare() {
        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(moonPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(3)
                .harvestedAt(LocalDateTime.of(2026, 6, 1, 12, 0)).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        DexSection moonSection = response.sections().stream()
                .filter(s -> s.speciesKey().equals("moon"))
                .findFirst().orElseThrow();

        assertThat(moonSection.rare()).isTrue();
        assertThat(moonSection.entries().get(0).collected()).isTrue();
        assertThat(moonSection.entries().get(0).monName()).isEqualTo("달빛씨");
        assertThat(moonSection.entries().get(1).collected()).isFalse();
    }

    @Test
    @DisplayName("도감 번호는 001~040 순서로 species 순서대로 연속 부여된다")
    void dexNumbers_sequentialAcrossSections() {
        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        int expected = 1;
        for (DexSection section : response.sections()) {
            for (DexEntry entry : section.entries()) {
                assertThat(entry.dexNumber()).isEqualTo(String.format("%03d", expected));
                expected++;
            }
        }
        assertThat(expected).isEqualTo(41);
    }

    @Test
    @DisplayName("plant name이 매핑되지 않는 식물의 수확은 도감에 영향을 주지 않는다")
    void unmappedPlant_ignoredInDex() {
        Plant unknownPlant = Plant.builder()
                .name("알 수 없는 식물").grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                .build();
        plantRepository.save(unknownPlant);

        plantItemRepository.save(PlantItem.builder()
                .userId(testUser.getId()).potId(testPot.getId()).plantId(unknownPlant.getId())
                .growthExp(1000).isHarvested(true).harvestedLevel(2)
                .harvestedAt(LocalDateTime.now()).build());

        CollectionDexResponse response = collectionService.getPlants(testUser.getId());

        assertThat(response.stats().collected()).isZero();
        assertThat(response.sections().stream().allMatch(s ->
                s.entries().stream().noneMatch(DexEntry::collected))).isTrue();
    }
}
