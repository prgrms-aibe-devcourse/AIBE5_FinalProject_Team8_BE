package com.Rootin.domain.garden.service;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
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
    private TilRepository tilRepository;

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private User otherUser;
    private Pot testPot;

    @BeforeEach
    void setUp() {
        // H2 데이터베이스의 테이블 PK 일관성 유지를 위해 시퀀스를 초기화합니다.
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE pot ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE watering_log ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE point_log ALTER COLUMN id RESTART WITH 1");

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
    }

    @Test
    @DisplayName("기본 물주기 처리 시 경험치와 포인트가 정상 연산되어 반영되고 이력이 남는다")
    void applyWateringSuccess() {
        // given
        // 실제 데이터 무결성 검증을 통과하기 위해 테스트용 TIL을 DB에 먼저 저장합니다.
        Til testTil = Til.create(testUser, "자바 공부방 TIL", "내용", testPot);
        tilRepository.save(testTil);

        int contentLength = 500; // 기본 경험치 100점 예상 (500 * 0.2)
        int streakDays = 0;      // 보너스 0% -> 최종 100 Exp, 10 Point 예상

        // when
        experienceService.applyWatering(testUser.getId(), testPot, contentLength, testTil.getId());

        // then
        // 1. 화분 경험치 증가 및 레벨업 검증 (105 Exp 쌓였으므로 2레벨 도달 예상, 오늘 포함 1일 스트릭 적용됨)
        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getTotalExp()).isEqualTo(105);
        assertThat(updatedPot.getLevel()).isEqualTo(2);

        // 2. 유저 포인트 적립 검증 (105 Exp의 10% = 10 P)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(10);

        // 3. 물주기 이력 저장 상태 및 세부 필드 검증
        List<WateringLog> logs = wateringLogRepository.findByPotId(testPot.getId());
        assertThat(logs).hasSize(1);
        WateringLog log = logs.get(0);
        assertThat(log.getUserId()).isEqualTo(testUser.getId());
        assertThat(log.getExpGained()).isEqualTo(105);
        assertThat(log.getPointGained()).isEqualTo(10);
        assertThat(log.getContentLength()).isEqualTo(500);
        assertThat(log.getStreakDays()).isEqualTo(1);
        assertThat(log.getAppliedMultiplier()).isEqualTo(1.05);
        assertThat(log.getBeforePotLevel()).isEqualTo(1);
        assertThat(log.getAfterPotLevel()).isEqualTo(2);
        assertThat(log.getBeforeTotalExp()).isEqualTo(0);
        assertThat(log.getAfterTotalExp()).isEqualTo(105);

        // 4. 포인트 적립 상세 이력(PointLog) 저장 상태 및 필드 검증
        List<PointLog> pointLogs = pointLogRepository.findAll();
        assertThat(pointLogs).hasSize(1);
        PointLog pointLog = pointLogs.get(0);
        assertThat(pointLog.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(pointLog.getReason()).isEqualTo(PointLogReason.TIL_WRITE);
        assertThat(pointLog.getAmount()).isEqualTo(10);
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

        // 실제 데이터 정합성 검사를 통과하기 위해 오늘 날짜로 발행되는 실제 TIL 포스트를 생성합니다.
        Til todayTil = Til.create(testUser, "오늘의 공부 내용", "내용", testPot);
        tilRepository.save(todayTil);

        int contentLength = 500; // 기본 100점
        // 오늘을 포함하여 스트릭 4일 -> 보너스 +20% (가중치 1.20) -> 최종 120 Exp 예상

        // when
        experienceService.applyWatering(testUser.getId(), testPot, contentLength, todayTil.getId());

        // then
        Pot updatedPot = potRepository.findById(testPot.getId()).orElseThrow();
        assertThat(updatedPot.getTotalExp()).isEqualTo(120);
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
        TilResponse response = tilService.create(testUser.getId(), request);

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
}
