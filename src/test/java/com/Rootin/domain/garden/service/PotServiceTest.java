package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@IntegrationTest
@Transactional
class PotServiceTest {

    @Autowired
    private PotService potService;

    @Autowired
    private GardenDashboardService gardenDashboardService;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private PlantItemRepository plantItemRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private TilRepository tilRepository;

    @Autowired
    private WateringLogRepository wateringLogRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        boolean defaultPlantExists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(
                        "기본 씨앗",
                        Grade.COMMON,
                        GrowthStage.SEED
                )
                .isPresent();

        if (!defaultPlantExists) {
            Plant defaultPlant = Plant.builder()
                    .name("기본 씨앗")
                    .grade(Grade.COMMON)
                    .growthStage(GrowthStage.SEED)
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
        Assertions.assertThrows(CustomException.class, () -> {
            potService.getPot(createdPot.getId(), otherId);
        });
    }

    @Test
    @DisplayName("사용자의 화분 목록을 요약 정보 DTO 목록으로 성공적으로 조회한다")
    void getPotsSummarySuccess() {
        // given
        Long userId = 100L;
        PotCreateRequest req1 = PotCreateRequest.builder().title("화분1").build();
        PotCreateRequest req2 = PotCreateRequest.builder().title("화분2").build();

        potService.createPot(userId, req1);
        potService.createPot(userId, req2);

        // when
        List<PotSummaryResponse> pots = potService.getPots(userId);

        // then
        assertThat(pots).hasSize(2);
        assertThat(pots.get(0).plantName()).isEqualTo("기본 씨앗");
        assertThat(pots.get(0).growthStage()).isEqualTo(GrowthStage.SEED);
    }

    @Test
    @DisplayName("중복 활성 PlantItem 데이터가 있어도 목록과 대시보드 조회는 실패하지 않는다")
    void gardenViewsDoNotFailWhenDuplicatedActivePlantItemsExist() {
        // given
        Long userId = 101L;
        PotResponse createdPot = potService.createPot(
                userId,
                PotCreateRequest.builder().title("중복 데이터 방어 화분").build()
        );

        Plant defaultPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("기본 씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElseThrow();

        plantItemRepository.save(PlantItem.builder()
                .userId(userId)
                .potId(createdPot.getId())
                .plantId(defaultPlant.getId())
                .build());

        // when & then
        assertThatCode(() -> potService.getPots(userId)).doesNotThrowAnyException();
        assertThatCode(() -> gardenDashboardService.getGardenDashboard(createdPot.getId(), userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("화분의 상세 대시보드 데이터를 모든 복합 수식 및 연계 통계와 함께 정상 조회한다")
    void getGardenDashboardSuccess() {
        // given
        // 1. 유저 저장
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("테스터")
                .build();
        userRepository.save(user);
        Long userId = user.getId();

        // 2. 화분 생성 (경험치 150 누적 -> 2레벨 예상)
        PotCreateRequest req = PotCreateRequest.builder()
                .title("대시보드 화분")
                .description("설명글")
                .build();
        PotResponse createdPot = potService.createPot(userId, req);

        Pot pot = potRepository.findById(createdPot.getId()).get();
        // 경험치 150 및 레벨 2 강제 변동 처리 (수식 검증 목적)
        pot.updateExperienceAndLevel(150, 2);
        potRepository.save(pot);

        // 3. 임시 TIL 발행 데이터 추가 (발행완료 2건)
        Til til1 = Til.create(user, "제목1", "내용1", pot);
        Til til2 = Til.create(user, "제목2", "내용2", pot);
        tilRepository.save(til1);
        tilRepository.save(til2);

        // 4. 물주기 로그 추가
        WateringLog log = WateringLog.builder()
                .userId(userId)
                .potId(pot.getId())
                .postId(til1.getId())
                .expGained(100)
                .pointGained(10)
                .contentLength(100)
                .streakDays(1)
                .appliedMultiplier(1.05)
                .beforePotLevel(1)
                .afterPotLevel(2)
                .beforeTotalExp(0)
                .afterTotalExp(100)
                .build();
        wateringLogRepository.save(log);

        // when
        GardenInfoResponse dashboard = gardenDashboardService.getGardenDashboard(pot.getId(), userId);

        // then
        assertThat(dashboard.potId()).isEqualTo(pot.getId());
        assertThat(dashboard.title()).isEqualTo("대시보드 화분");
        assertThat(dashboard.description()).isEqualTo("설명글");
        assertThat(dashboard.level()).isEqualTo(2);
        assertThat(dashboard.totalExp()).isEqualTo(150);
        // 2레벨 구간 시작 100이므로 150 - 100 = 50 구간 획득 경험치 예상
        assertThat(dashboard.currentLevelExp()).isEqualTo(50);
        // 2레벨 구간 총 요구량 200 예상
        assertThat(dashboard.nextLevelExpRequired()).isEqualTo(200);
        // (50/200)*100 = 25.0%
        assertThat(dashboard.progressPercentage()).isEqualTo(25.0);
        // 발행완료 TIL 2개
        assertThat(dashboard.totalTilCount()).isEqualTo(2);
        // 최근 물주기 기록 있음
        assertThat(dashboard.lastWateredAt()).isNotNull();
        // 심긴 식물 정보 검증 (계산된 성장 단계인 SEED로 매핑)
        assertThat(dashboard.plant().name()).isEqualTo("기본 씨앗");
        assertThat(dashboard.plant().growthStage()).isEqualTo(GrowthStage.SEED);
    }
}
