package com.Rootin.global.config.seeder;

import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.global.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlantMasterSeederTest {

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private S3Service s3Service;

    private PlantMasterSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new PlantMasterSeeder(plantRepository, s3Service);
    }

    @Test
    @DisplayName("seed — 데이터가 없으면 8종 × 5단계 = 40개 식물을 저장한다")
    void seed_savesAllPlants_whenEmpty() {
        // given
        given(plantRepository.findFirstByNameAndGradeAndGrowthStage(
                PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED))
                .willReturn(Optional.empty());
        given(s3Service.getFileUrl(anyString()))
                .willAnswer(inv -> "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/" + inv.getArgument(0));

        // when
        seeder.seed();

        // then
        ArgumentCaptor<Plant> captor = ArgumentCaptor.forClass(Plant.class);
        verify(plantRepository, org.mockito.Mockito.times(40)).save(captor.capture());

        List<Plant> saved = captor.getAllValues();
        assertThat(saved).hasSize(40);
    }

    @Test
    @DisplayName("seed — 저장되는 imageUrl이 plants/{prefix}/{stage_name}.svg 형식이다")
    void seed_imageUrl_hasCorrectPathFormat() {
        // given
        given(plantRepository.findFirstByNameAndGradeAndGrowthStage(
                PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED))
                .willReturn(Optional.empty());
        given(s3Service.getFileUrl(anyString()))
                .willAnswer(inv -> "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/" + inv.getArgument(0));

        // when
        seeder.seed();

        // then
        ArgumentCaptor<Plant> captor = ArgumentCaptor.forClass(Plant.class);
        verify(plantRepository, org.mockito.Mockito.times(40)).save(captor.capture());

        List<Plant> saved = captor.getAllValues();

        // 모든 imageUrl이 plants/{prefix}/{stage_name}.svg 형식인지 검증
        assertThat(saved).allSatisfy(plant ->
                assertThat(plant.getImageUrl())
                        .matches("https://team8-rootin-s3\\.s3\\.ap-northeast-2\\.amazonaws\\.com/plants/[^/]+/(01_seed|02_sprout|03_leaf|04_flower|05_bloom)\\.svg")
        );
    }

    @Test
    @DisplayName("seed — 5단계 파일명이 순서대로 01_seed ~ 05_bloom이다")
    void seed_stageFileNames_areInOrder() {
        // given
        given(plantRepository.findFirstByNameAndGradeAndGrowthStage(
                PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED))
                .willReturn(Optional.empty());
        given(s3Service.getFileUrl(anyString()))
                .willAnswer(inv -> "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/" + inv.getArgument(0));

        // when
        seeder.seed();

        // then
        ArgumentCaptor<Plant> captor = ArgumentCaptor.forClass(Plant.class);
        verify(plantRepository, org.mockito.Mockito.times(40)).save(captor.capture());

        // bolt 종의 5단계 URL만 추출해서 순서 확인
        List<String> boltUrls = captor.getAllValues().stream()
                .map(Plant::getImageUrl)
                .filter(url -> url.contains("/bolt/"))
                .toList();

        assertThat(boltUrls).containsExactly(
                "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/plants/bolt/01_seed.svg",
                "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/plants/bolt/02_sprout.svg",
                "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/plants/bolt/03_leaf.svg",
                "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/plants/bolt/04_flower.svg",
                "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/plants/bolt/05_bloom.svg"
        );
    }

    @Test
    @DisplayName("seed — 이미 데이터가 있으면 저장하지 않는다")
    void seed_skips_whenDataExists() {
        // given
        Plant existing = Plant.builder()
                .name(PlantMasterSeeder.DEFAULT_PLANT_NAME)
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("any")
                .silhouetteUrl(null)
                .build();
        given(plantRepository.findFirstByNameAndGradeAndGrowthStage(
                PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED))
                .willReturn(Optional.of(existing));

        // when
        seeder.seed();

        // then
        verify(plantRepository, never()).save(any());
    }
}
