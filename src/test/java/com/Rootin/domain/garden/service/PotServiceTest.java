package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.global.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class PotServiceTest {

    @Autowired
    private PotService potService;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private PlantItemRepository plantItemRepository;

    @Autowired
    private com.Rootin.domain.plant.repository.PlantRepository plantRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        boolean defaultPlantExists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(
                        "기본 씨앗",
                        com.Rootin.domain.plant.entity.enums.Grade.COMMON,
                        com.Rootin.domain.plant.entity.enums.GrowthStage.SEED
                )
                .isPresent();

        if (!defaultPlantExists) {
            com.Rootin.domain.plant.entity.Plant defaultPlant = com.Rootin.domain.plant.entity.Plant.builder()
                    .name("기본 씨앗")
                    .grade(com.Rootin.domain.plant.entity.enums.Grade.COMMON)
                    .growthStage(com.Rootin.domain.plant.entity.enums.GrowthStage.SEED)
                    .build();
            plantRepository.save(defaultPlant);
        }
    }

    @Test
    @DisplayName("화분을 새로 생성하면 기본 레벨 1과 기본 씨앗(PlantItem)이 함께 매핑되어 성공적으로 저장된다")
    void createPotSuccess() {
        // given
        Long userId = 1L;
        PotCreateRequest request = PotCreateRequest.builder()
                .title("자바 공부 화분")
                .description("자바 기초부터 마스터까지")
                .build();

        // when
        PotResponse response = potService.createPot(userId, request);

        // then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("자바 공부 화분");
        assertThat(response.getDescription()).isEqualTo("자바 기초부터 마스터까지");
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getTotalExp()).isEqualTo(0);

        // plant_item 테이블에 수확되지 않은 기본 식물 데이터가 잘 매핑되어 들어갔는지 검증
        boolean plantItemExists = plantItemRepository.findByPotIdAndIsHarvestedFalse(response.getId()).isPresent();
        assertThat(plantItemExists).isTrue();
    }

    @Test
    @DisplayName("본인의 화분 상세 정보는 정상적으로 조회된다")
    void getPotSuccess() {
        // given
        Long userId = 1L;
        PotCreateRequest request = PotCreateRequest.builder()
                .title("테스트 화분")
                .build();
        PotResponse createdPot = potService.createPot(userId, request);

        // when
        PotResponse response = potService.getPot(createdPot.getId(), userId);

        // then
        assertThat(response.getId()).isEqualTo(createdPot.getId());
    }

    @Test
    @DisplayName("다른 사용자의 화분을 상세 조회하면 권한 예외(FORBIDDEN)가 발생한다")
    void getPotForbidden() {
        // given
        Long ownerId = 1L;
        Long otherId = 2L;
        PotCreateRequest request = PotCreateRequest.builder()
                .title("주인의 화분")
                .build();
        PotResponse createdPot = potService.createPot(ownerId, request);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(com.Rootin.global.exception.CustomException.class, () -> {
            potService.getPot(createdPot.getId(), otherId);
        });
    }
}
