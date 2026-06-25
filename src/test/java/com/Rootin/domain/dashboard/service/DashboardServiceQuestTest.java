package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.QuestDto;
import com.Rootin.domain.dashboard.dto.QuestResponse;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.til.entity.Tag;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Set;

@IntegrationTest
@Transactional
class DashboardServiceQuestTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private UserRepository userRepository;
    @Autowired private PotRepository potRepository;
    @Autowired private TilRepository tilRepository;
    @Autowired private TilTagRepository tilTagRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private WateringLogRepository wateringLogRepository;
    @Autowired private PointLogRepository pointLogRepository;
    @Autowired private EntityManager em;

    private User user;
    private Pot pot;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("quest-test@test.com")
                .nickname("퀘스트테스터")
                .build());

        pot = potRepository.save(Pot.builder()
                .userId(user.getId())
                .title("퀘스트 화분")
                .level(1)
                .totalExp(0)
                .isDisplayed(false)
                .build());
    }

    /** WateringLog를 오늘 날짜로 저장 (wateredAt 은 @PrePersist 로 자동 설정) */
    private void saveWateringLog(Long postId, int contentLength) {
        wateringLogRepository.save(WateringLog.builder()
                .userId(user.getId())
                .potId(pot.getId())
                .postId(postId)
                .expGained(10)
                .pointGained(5)
                .contentLength(contentLength)
                .streakDays(1)
                .appliedMultiplier(1.0)
                .beforePotLevel(1)
                .afterPotLevel(1)
                .beforeTotalExp(0)
                .afterTotalExp(10)
                .build());
    }

    private void saveWateringLogAt(Long postId, int contentLength, LocalDate wateredDate) {
        WateringLog log = wateringLogRepository.save(WateringLog.builder()
                .userId(user.getId())
                .potId(pot.getId())
                .postId(postId)
                .expGained(10)
                .pointGained(5)
                .contentLength(contentLength)
                .streakDays(1)
                .appliedMultiplier(1.0)
                .beforePotLevel(1)
                .afterPotLevel(1)
                .beforeTotalExp(0)
                .afterTotalExp(10)
                .build());
        em.flush();
        em.createNativeQuery("UPDATE watering_log SET watered_at = :wateredAt WHERE id = :id")
                .setParameter("wateredAt", wateredDate.atTime(9, 0))
                .setParameter("id", log.getId())
                .executeUpdate();
        em.flush();
        em.clear();
    }

    // ──────────────────────────────────────────────
    // Q1: 오늘 TIL >= 1개
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Q1 - 오늘 WateringLog가 없으면 미달성")
    void q1_fail_noWateringLog() {
        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(0).id()).isEqualTo("Q1");
        assertThat(response.quests().get(0).done()).isFalse();
    }

    @Test
    @DisplayName("Q1 - 오늘 WateringLog가 1개 이상이면 달성")
    void q1_success_hasWateringLog() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 50);

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(0).done()).isTrue();
    }

    // ──────────────────────────────────────────────
    // Q2: 오늘 TIL에 TilTag >= 1
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Q2 - 오늘 발행된 TIL에 태그가 없으면 미달성")
    void q2_fail_noTag() {
        tilRepository.save(Til.create(user, "태그없는 TIL", "내용", pot));

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(1).id()).isEqualTo("Q2");
        assertThat(response.quests().get(1).done()).isFalse();
    }

    @Test
    @DisplayName("Q2 - 오늘 발행된 TIL에 태그가 1개 이상이면 달성")
    void q2_success_hasTag() {
        Til til = tilRepository.save(Til.create(user, "태그있는 TIL", "내용", pot));
        Tag tag = tagRepository.save(Tag.create("Spring"));
        tilTagRepository.save(TilTag.of(til, tag));

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(1).done()).isTrue();
    }

    // ──────────────────────────────────────────────
    // Q3: 오늘 총 글자수 >= 200
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Q3 - 오늘 총 글자수가 199자면 미달성")
    void q3_fail_under200() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 199);

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(2).id()).isEqualTo("Q3");
        assertThat(response.quests().get(2).done()).isFalse();
    }

    @Test
    @DisplayName("Q3 - 오늘 총 글자수가 정확히 200자면 달성")
    void q3_success_exactly200() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 200);

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(2).done()).isTrue();
    }

    @Test
    @DisplayName("Q3 - 여러 WateringLog의 글자수 합이 200 이상이면 달성")
    void q3_success_sumOver200() {
        Til til1 = tilRepository.save(Til.create(user, "TIL1", "내용", pot));
        Til til2 = tilRepository.save(Til.create(user, "TIL2", "내용", pot));
        saveWateringLog(til1.getId(), 100);
        saveWateringLog(til2.getId(), 101);

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.quests().get(2).done()).isTrue();
    }

    // ──────────────────────────────────────────────
    // 전체 구조 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("잔디 그래프 maxStreak는 그래프 조회 기간 안의 기록만 기준으로 계산한다")
    void grassGraph_maxStreak_usesOnlyGraphRangeDates() {
        LocalDate oldDay = LocalDate.now().minusYears(2);
        LocalDate currentRangeDay = LocalDate.now().minusDays(1);

        Til oldTil1 = tilRepository.save(Til.create(user, "오래된 TIL 1", "내용", pot));
        Til oldTil2 = tilRepository.save(Til.create(user, "오래된 TIL 2", "내용", pot));
        Til recentTil = tilRepository.save(Til.create(user, "최근 TIL", "내용", pot));

        saveWateringLogAt(oldTil1.getId(), 50, oldDay);
        saveWateringLogAt(oldTil2.getId(), 50, oldDay.plusDays(1));
        saveWateringLogAt(recentTil.getId(), 50, currentRangeDay);

        var response = dashboardService.getGrassGraph(user.getId());

        assertThat(response.maxStreak()).isEqualTo(1);
        assertThat(response.cells()).extracting(cell -> cell.date())
                .containsExactly(currentRangeDay);
    }

    @Test
    @DisplayName("현재 스트릭은 물주기 로그 날짜가 아니라 TIL 발행 날짜 기준으로 계산한다")
    void currentStreak_usesPublishedTilDates() {
        Til todayTil = tilRepository.save(Til.create(user, "오늘 발행 TIL", "내용", pot));
        saveWateringLogAt(todayTil.getId(), 50, LocalDate.now().minusDays(10));

        var grass = dashboardService.getGrassGraph(user.getId());
        var personalStats = dashboardService.getPersonalStats(user.getId());

        assertThat(grass.currentStreak()).isEqualTo(1);
        assertThat(personalStats.currentStreak()).isEqualTo(1);
        assertThat(personalStats.longestStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("3개 퀘스트 모두 달성 시 earnedToday = totalToday = 100")
    void earnedPoints_allDone_is_100() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 200);

        Tag tag = tagRepository.save(Tag.create("Java"));
        tilTagRepository.save(TilTag.of(til, tag));

        QuestResponse response = dashboardService.getQuests(user.getId());

        assertThat(response.earnedToday()).isEqualTo(100);
        assertThat(response.totalToday()).isEqualTo(100);
    }

    // ──────────────────────────────────────────────
    // 실제 포인트 적립 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("퀘스트 달성 시 user.point에 실제로 포인트가 적립된다")
    void questAward_addsPointToUser() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 200);

        Tag tag = tagRepository.save(Tag.create("Kotlin"));
        tilTagRepository.save(TilTag.of(til, tag));

        dashboardService.getQuests(user.getId());

        // 1차 캐시를 비우고 DB에서 재조회하여 실제 point 변경 확인
        em.flush();
        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(100); // Q1(50) + Q2(30) + Q3(20)
    }

    @Test
    @DisplayName("getQuests()를 두 번 호출해도 포인트는 한 번만 적립된다 (중복 방지)")
    void questAward_noDuplicatePoint() {
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 200);

        Tag tag = tagRepository.save(Tag.create("Redis"));
        tilTagRepository.save(TilTag.of(til, tag));

        dashboardService.getQuests(user.getId());
        dashboardService.getQuests(user.getId()); // 두 번째 호출

        em.flush();
        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(100); // 200이 아닌 100
    }

    @Test
    @DisplayName("퀘스트 포인트 로그는 동일 user/reason/date에 대해 한 번만 삽입된다")
    void questLogInsertIfAbsent_preventsDuplicateByUniqueKey() {
        LocalDate today = LocalDate.now();

        int firstInsert = pointLogRepository.insertQuestLogIfAbsent(
                user.getId(), PointLogReason.QUEST_Q1.name(), 50, today);
        int duplicateInsert = pointLogRepository.insertQuestLogIfAbsent(
                user.getId(), PointLogReason.QUEST_Q1.name(), 50, today);

        em.flush();
        em.clear();

        Set<PointLogReason> awardedToday = pointLogRepository.findQuestReasonsByUserIdAndAwardedDate(
                user.getId(),
                today,
                Set.of(PointLogReason.QUEST_Q1, PointLogReason.QUEST_Q2, PointLogReason.QUEST_Q3)
        );
        long savedCount = pointLogRepository.countByUserIdAndReasonAndAwardedDate(
                user.getId(), PointLogReason.QUEST_Q1, today);

        assertThat(firstInsert).isEqualTo(1);
        assertThat(duplicateInsert).isZero();
        assertThat(awardedToday).containsExactly(PointLogReason.QUEST_Q1);
        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("미달성 퀘스트는 포인트가 지급되지 않는다")
    void questAward_notDoneQuestNotAwarded() {
        // Q1만 달성 (WateringLog 있음, 태그 없음, 글자수 50)
        Til til = tilRepository.save(Til.create(user, "제목", "내용", pot));
        saveWateringLog(til.getId(), 50);

        dashboardService.getQuests(user.getId());

        em.flush();
        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(50); // Q1(50)만 적립
    }
}
