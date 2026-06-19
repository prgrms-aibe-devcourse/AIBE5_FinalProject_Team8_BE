package com.Rootin.domain.garden.service;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
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
import com.Rootin.domain.til.dto.request.DraftSaveRequest;
import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.service.TilService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
@Transactional
class ExperienceServiceTest {

    @Autowired
    private ExperienceService experienceService;

    @Autowired
    private TilService tilService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private WateringLogRepository wateringLogRepository;

    @Autowired
    private PlantItemRepository plantItemRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private TilRepository tilRepository;

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    private User testUser;
    private User otherUser;
    private Pot testPot;

    @BeforeEach
    void setUp() {
        // MySQL AUTO_INCREMENT 카운터를 초기화하여 테스트 간 PK 일관성 유지
        // DDL이 묵시적 커밋을 유발하지만 이후 DML은 Spring @Transactional 롤백 범위에 포함됨
        jdbcTemplate.execute("ALTER TABLE users AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE pot AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE posts AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE watering_log AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE point_log AUTO_INCREMENT = 1");

        // 1. 테스트용 유저 생성 및 저장
        testUser = User.builder()
                .email("yunseok@test.com")
                .nickname("윤석")
                .point(0)
                .build();
        userRepository.save(testUser);

        otherUser = User.builder()
                .email("other@test.com")
                .nickname("다른사람")
                .point(0)
                .build();
        userRepository.save(otherUser);

        // 2. 테스트용 화분 생성 및 저장
        testPot = Pot.builder()
                .userId(testUser.getId())
                .title("스프링 정복 화분")
                .description("스프링 마스터하기")
                .level(1)
                .totalExp(0)
                .build();
        potRepository.save(testPot);

        // 3. 테스트용 식물 마스터 데이터 및 식물 아이템 생성
        Plant testPlant = Plant.builder()
                .name("기본 씨앗")
                .grade(Grade.COMMON)
                .growthStage(GrowthStage.SEED)
                .imageUrl("seed_image_url")
                .silhouetteUrl("seed_silhouette_url")
                .build();
        plantRepository.save(testPlant);

        PlantItem testPlantItem = PlantItem.builder()
                .userId(testUser.getId())
                .potId(testPot.getId())
                .plantId(testPlant.getId())
                .growthExp(0)
                .isHarvested(false)
                .build();
        plantItemRepository.save(testPlantItem);
    }

    @Test
    @DisplayName("기본 물주기 처리 시 경험치가 정상 연산되어 반영되고 이력이 남는다 (포인트는 오늘의 목표에서만 지급)")
    void applyWateringSuccess() {
        // given
        Til testTil = Til.create(testUser, "자바 공부방 TIL", "내용", testPot);
        tilRepository.save(testTil);

        int contentLength = 500; // 기본 경험치 100점 예상 (500 * 0.2), 스트릭 0일 -> 배율 1.0

        // when
        experienceService.applyWatering(testUser.getId(), testPot, contentLength, testTil.getId());

        // then
        // 1. 화분 경험치 증가 및 레벨업 검증 (100 Exp -> 2레벨)
        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getTotalExp()).isEqualTo(100);
        assertThat(updatedPot.getLevel()).isEqualTo(2);

        // 2. 식물 경험치 증가 검증
        PlantItem updatedPlantItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(testPot.getId()).orElseThrow();
        assertThat(updatedPlantItem.getGrowthExp()).isEqualTo(100);

        // 3. 포인트는 오늘의 목표(DashboardService)에서만 지급 — TIL 작성 시 user.point 변화 없음
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(0);

        // 4. 물주기 이력 저장 상태 및 세부 필드 검증
        List<WateringLog> logs = wateringLogRepository.findByPotId(testPot.getId());
        assertThat(logs).hasSize(1);
        WateringLog log = logs.get(0);
        assertThat(log.getUserId()).isEqualTo(testUser.getId());
        assertThat(log.getExpGained()).isEqualTo(100);
        assertThat(log.getPointGained()).isEqualTo(0); // 포인트는 퀘스트에서 지급
        assertThat(log.getContentLength()).isEqualTo(500);
        assertThat(log.getStreakDays()).isEqualTo(0);
        assertThat(log.getAppliedMultiplier()).isEqualTo(1.0);
        assertThat(log.getBeforePotLevel()).isEqualTo(1);
        assertThat(log.getAfterPotLevel()).isEqualTo(2);
        assertThat(log.getBeforeTotalExp()).isEqualTo(0);
        assertThat(log.getAfterTotalExp()).isEqualTo(100);

        // 5. TIL 작성으로 인한 PointLog 없음
        List<PointLog> pointLogs = pointLogRepository.findAll();
        assertThat(pointLogs).isEmpty();
    }

    @Test
    @DisplayName("연속 작성일(스트릭)이 있는 경우 경험치에 가중치 배율이 안전하게 단리로 가산된다")
    void applyWateringWithStreakBonus() {
        // given
        // 어제, 그저께, 그그저께 3일 연속으로 작성한 가짜 데이터를 생성해 삽입합니다.
        LocalDateTime now = LocalDateTime.now();
        Til oldTil1 = Til.create(testUser, "1일전 TIL", "내용", testPot);
        tilRepository.save(oldTil1);
        jdbcTemplate.update("UPDATE til SET published_at = ? WHERE post_id = ?", now.minusDays(1), oldTil1.getId());

        Til oldTil2 = Til.create(testUser, "2일전 TIL", "내용", testPot);
        tilRepository.save(oldTil2);
        jdbcTemplate.update("UPDATE til SET published_at = ? WHERE post_id = ?", now.minusDays(2), oldTil2.getId());

        Til oldTil3 = Til.create(testUser, "3일전 TIL", "내용", testPot);
        tilRepository.save(oldTil3);
        jdbcTemplate.update("UPDATE til SET published_at = ? WHERE post_id = ?", now.minusDays(3), oldTil3.getId());

        // jdbcTemplate으로 변경한 published_at이 JPA L1 캐시에 의해 무시되지 않도록
        // 캐시를 플러시 후 초기화합니다. 이후 JPQL 쿼리는 DB에서 신선한 값을 읽습니다.
        em.flush();
        em.clear();

        // em.clear() 후 testPot가 detached 상태가 되므로 re-fetch하여 관리 상태로 복원합니다.
        // detached 엔티티는 JPA dirty checking 대상이 아니라 서비스 내 pot.addExp() 변경이 DB에 반영되지 않습니다.
        testPot = potRepository.findById(testPot.getId()).orElseThrow();

        // 실제 데이터 정합성 검사를 통과하기 위해 오늘 날짜로 발행되는 실제 TIL 포스트를 생성합니다.
        Til todayTil = Til.create(testUser, "오늘의 공부 내용", "내용", testPot);
        tilRepository.save(todayTil);

        int contentLength = 500; // 기본 100점
        // 어제 기준 연속 스트릭 3일 -> 보너스 +15% (가중치 1.15) -> 최종 115 Exp 예상

        // when
        experienceService.applyWatering(testUser.getId(), testPot, contentLength, todayTil.getId());

        // 서비스가 변경한 pot dirty 상태를 DB에 강제 반영 후 캐시를 초기화합니다.
        // L1 캐시 hit/miss 여부와 관계없이 DB 기준으로 확인하여 환경 차이에 의한 플레이키 테스트를 방지합니다.
        em.flush();
        em.clear();

        // then
        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getTotalExp()).isEqualTo(115);
    }

    @Test
    @DisplayName("TIL 서비스에서 TIL 저장 성공 시 자동으로 물주기 로직이 연쇄 실행(트리거)된다")
    void tilCreateTriggersWatering() {
        // given
        // TilCreateRequest는 자바 레코드이므로 builder가 없고 직접 생성자를 호출해 객체를 만듭니다.
        TilCreateRequest request = new TilCreateRequest(
                "자바 개발 꿀팁",
                "이것은 자바 공부 내용입니다. 글자 수를 테스트하기 위한 긴 텍스트입니다. " +
                "충분한 양의 텍스트가 채워져야 기본 경험치가 쌓이게 됩니다.", // 공백 포함 83글자 예상
                testPot.getId(),
                List.of("Java", "Programming")
        );

        // when
        TilResponse response = tilService.create(testUser.getId(), request, null);

        // then
        // 1. TIL이 정상 생성 확인 (레코드는 getId()가 아니라 tilId() 메소드를 제공함)
        assertThat(response.tilId()).isNotNull();

        // 2. 화분에 경험치가 쌓였는지 확인
        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getTotalExp()).isGreaterThan(0);

        // 3. 물주기 이력이 1건 추가되었는지 확인
        List<WateringLog> logs = wateringLogRepository.findByPotId(testPot.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getPostId()).isEqualTo(response.tilId());
    }

    @Test
    @DisplayName("TIL 본문에 서식(HTML 태그)이 있어도 경험치는 태그·공백을 제외한 순수 텍스트 글자 수로 산정된다")
    void tilCreateUsesVisibleTextLengthForExp() {
        // given: 가시 텍스트는 "오늘배운것"(5자)이지만 HTML 원문은 태그 때문에 훨씬 길다
        String html = "<p><strong>오늘</strong> <code>배운</code> 것</p>";
        TilCreateRequest request = new TilCreateRequest("서식 TIL", html, testPot.getId(), List.of("Java"));

        // when
        TilResponse response = tilService.create(testUser.getId(), request, null);

        em.flush();
        em.clear();

        // then: WateringLog에는 HTML 원문 길이가 아니라 가시 글자 수(5)가 기록된다
        List<WateringLog> logs = wateringLogRepository.findByPotId(testPot.getId());
        assertThat(logs).hasSize(1);
        WateringLog log = logs.get(0);
        assertThat(log.getPostId()).isEqualTo(response.tilId());
        assertThat(log.getContentLength()).isEqualTo(5);
        assertThat(log.getContentLength()).isLessThan(html.length());
        // 경험치 = floor(min(5 * 0.2, 300) * 1.0) = 1 (첫 작성이라 스트릭 0일)
        assertThat(log.getExpGained()).isEqualTo(1);
    }

    @Test
    @DisplayName("가시 텍스트가 없는 본문(서식 태그만 존재)은 경험치/물주기 이력을 만들지 않는다")
    void tilCreateWithNoVisibleTextSkipsWatering() {
        // given: @NotBlank는 통과하지만 가시 글자 수가 0인 본문
        TilCreateRequest request = new TilCreateRequest("빈 본문 TIL", "<p></p>", testPot.getId(), List.of());

        // when
        TilResponse response = tilService.create(testUser.getId(), request, null);

        em.flush();
        em.clear();

        // then: TIL은 생성되지만 물주기 이력이 남지 않고(0-exp 로그가 post_id를 선점하지 않음) 화분 경험치도 그대로다
        assertThat(response.tilId()).isNotNull();
        assertThat(wateringLogRepository.findByPotId(testPot.getId())).isEmpty();
        Pot pot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(pot.getTotalExp()).isZero();
    }

    @Test
    @DisplayName("본인의 화분이 아닌 다른 사용자의 화분에 물주기를 수행하면 FORBIDDEN 에러가 발생한다")
    void applyWateringForbidden() {
        // given
        // otherUser가 testUser 소유의 화분에 물주기를 시도합니다.
        Long attackerId = otherUser.getId();
        
        // 데이터 정합성 검증(TIL의 실제 존재 검사)을 패스하기 위한 testUser 소유의 실제 TIL을 생성합니다.
        Til testTil = Til.create(testUser, "다른 유저의 TIL", "내용", testPot);
        tilRepository.save(testTil);

        // when & then
        assertThrows(CustomException.class, () -> {
            experienceService.applyWatering(attackerId, testPot, 500, testTil.getId());
        });
    }

    @Test
    @DisplayName("동일한 TIL ID로 물주기를 두 번 수행하려고 하면 BAD_REQUEST 에러가 발생한다")
    void applyWateringDuplicateFail() {
        // given
        // 데이터 정합성 검사를 정상 통과하기 위한 실제 TIL을 생성합니다.
        Til testTil = Til.create(testUser, "중복 체크용 TIL", "내용", testPot);
        tilRepository.save(testTil);
        
        experienceService.applyWatering(testUser.getId(), testPot, 500, testTil.getId());

        // when & then (두 번째 물주기 시도 시 예외 발생 검증)
        CustomException exception = assertThrows(CustomException.class, () -> {
            experienceService.applyWatering(testUser.getId(), testPot, 500, testTil.getId());
        });
        
        assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("이미 물주기가 완료된 TIL입니다.");
    }

    @Test
    @DisplayName("타인 화분에 TIL을 작성하려고 하면 저장 전에 FORBIDDEN 에러가 발생한다")
    void tilCreateForbiddenWhenPotOwnerMismatch() {
        TilCreateRequest request = new TilCreateRequest(
                "타인 화분 작성 시도",
                "권한이 없어야 하는 내용",
                testPot.getId(),
                List.of("Security")
        );

        CustomException exception = assertThrows(CustomException.class, () ->
                tilService.create(otherUser.getId(), request, null)
        );

        assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        assertThat(wateringLogRepository.findByPotId(testPot.getId())).isEmpty();
    }

    @Test
    @DisplayName("타인 화분에 임시저장을 시도하면 FORBIDDEN 에러가 발생한다")
    void saveDraftForbiddenWhenPotOwnerMismatch() {
        DraftSaveRequest request = new DraftSaveRequest(
                testPot.getId(),
                "타인 화분 임시저장",
                "권한이 없어야 하는 초안",
                List.of("Draft")
        );

        CustomException exception = assertThrows(CustomException.class, () ->
                tilService.saveDraft(otherUser.getId(), request, null)
        );

        assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
    }
}
