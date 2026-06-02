package com.Rootin.global.config;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.til.entity.Tag;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DatabaseSeeder {

    private static final String DEFAULT_PLANT_NAME = "기본 씨앗";
    private static final String TEST_EMAIL = "test@rootin.com";

    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final TilRepository tilRepository;
    private final TagRepository tagRepository;
    private final TilTagRepository tilTagRepository;
    private final PointLogRepository pointLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        log.info("=== DB 시드 데이터 검사 시작 ===");
        seedPlantMaster();
        seedTestData();
        log.info("=== DB 시드 데이터 완료 ===");
    }

    // ── 1. 식물 마스터 데이터 ──────────────────────────────────────────
    private void seedPlantMaster() {
        boolean exists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .isPresent();
        if (!exists) {
            plantRepository.save(Plant.builder().name(DEFAULT_PLANT_NAME).grade(Grade.COMMON)
                    .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
            plantRepository.save(Plant.builder().name("달빛씨앗").grade(Grade.RARE)
                    .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
            plantRepository.save(Plant.builder().name("버섯씨앗").grade(Grade.COMMON)
                    .growthStage(GrowthStage.SEED).imageUrl(null).silhouetteUrl(null).build());
            log.info("식물 마스터 데이터 저장 완료");
        }
    }

    // ── 2. 테스트 데이터 (12개월치 정합성) ──────────────────────────────
    private void seedTestData() {
        if (userRepository.findByEmail(TEST_EMAIL).isPresent()) {
            log.info("테스트 데이터 이미 존재. 스킵.");
            return;
        }
        log.info("테스트 데이터 생성 시작 (12개월치)...");

        // ── 유저 ──────────────────────────────────────────────────────
        User user = userRepository.save(User.builder()
                .email(TEST_EMAIL).password(passwordEncoder.encode("test1234"))
                .nickname("루틴이").bio("루틴처럼 기록하고, 뿌리처럼 깊어지는 중.")
                .role(Role.USER).provider(Provider.LOCAL).point(0).build());

        // ── 식물 마스터 조회 ───────────────────────────────────────────
        Plant defaultPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .orElseThrow();
        Plant rarePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("달빛씨앗", Grade.RARE, GrowthStage.SEED)
                .orElse(defaultPlant);
        Plant mushroomPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("버섯씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElse(defaultPlant);

        // ── 화분 생성 ──────────────────────────────────────────────────
        Pot codingPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("코딩").description("매일 한 가지씩 배우는 코딩 기록")
                .level(1).totalExp(0).isDisplayed(true).build());
        Pot englishPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("영어").description("영어 학습 기록")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot readingPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("독서").description("책에서 건진 문장과 생각")
                .level(1).totalExp(0).isDisplayed(false).build());

        LocalDate today = LocalDate.now();

        // ── PlantItem 설정 ─────────────────────────────────────────────
        // 코딩 라운드1: 12달 전 ~ 6달 전 (만개, 수확 완료 → 식물도감에 기본씨앗 수집됨)
        PlantItem coding1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId())
                .isHarvested(true).harvestedLevel(10).growthExp(1000).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, harvested_at=? WHERE id=?",
                today.minusMonths(12).atTime(10, 0),
                today.minusMonths(6).atTime(20, 0), coding1.getId());

        // 코딩 라운드2: 6달 전 ~ 현재 (새싹 성장 중, growthExp=250)
        PlantItem coding2 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(6).atTime(20, 1), 250, coding2.getId());

        // 영어: 12달 전 ~ 현재 (개화 성장 중, growthExp=850)
        PlantItem english1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(englishPot.getId()).plantId(rarePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(12).atTime(10, 0), 850, english1.getId());

        // 독서: 3달 전 ~ 현재 (씨앗 단계, growthExp=80)
        PlantItem reading1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(readingPot.getId()).plantId(mushroomPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(3).atTime(10, 0), 80, reading1.getId());

        // ── 태그 ──────────────────────────────────────────────────────
        Tag javaTag   = getOrCreateTag("Java");
        Tag springTag = getOrCreateTag("Spring");
        Tag reactTag  = getOrCreateTag("React");
        Tag engTag    = getOrCreateTag("영어");
        Tag gramTag   = getOrCreateTag("문법");
        Tag bookTag   = getOrCreateTag("독서");
        Tag algoTag   = getOrCreateTag("알고리즘");

        // ── 월별 데이터 정의 ──────────────────────────────────────────
        // {monthsAgo, pot(0=코딩,1=영어,2=독서), charCount, tagIdx(0~6), tilCount}
        // tagIdx: 0=Java,1=Spring,2=React,3=영어,4=문법,5=독서,6=알고리즘
        final Tag[] TAGS = {javaTag, springTag, reactTag, engTag, gramTag, bookTag, algoTag};
        final Pot[] POTS = {codingPot, englishPot, readingPot};
        int[][] plan = {
            // Month -12: Java/Spring 입문 + 영어 강세
            {12,0,500,0,6}, {12,0,600,1,3}, {12,1,400,3,5}, {12,1,400,4,3},
            // Month -11: Java/Spring 심화
            {11,0,520,0,6}, {11,0,620,1,4}, {11,1,400,3,5}, {11,1,400,4,4},
            // Month -10: Spring 집중
            {10,0,600,0,5}, {10,0,650,1,5}, {10,1,420,3,6}, {10,1,400,4,4},
            // Month -9: React 등장
            {9,0,600,0,5}, {9,0,600,1,4}, {9,0,700,2,3}, {9,1,420,3,6}, {9,1,400,4,4},
            // Month -8: React 성장
            {8,0,500,0,3}, {8,0,600,1,4}, {8,0,720,2,6}, {8,1,420,3,5}, {8,1,400,4,4},
            // Month -7: React 집중 (코딩 라운드1 마지막)
            {7,0,500,0,2}, {7,0,550,1,3}, {7,0,750,2,8}, {7,1,420,3,5}, {7,1,400,4,3},
            // Month -6: 코딩 라운드2 시작, React 지속
            {6,0,500,0,2}, {6,0,720,2,8}, {6,1,400,3,5}, {6,1,380,4,3},
            // Month -5: React + 알고리즘 시작
            {5,0,500,0,2}, {5,0,750,2,9}, {5,0,820,6,2}, {5,1,400,3,4}, {5,1,370,4,3},
            // Month -4: 알고리즘 성장
            {4,0,500,0,2}, {4,0,760,2,7}, {4,0,870,6,5}, {4,1,400,3,4}, {4,1,360,4,3},
            // Month -3: 알고리즘 + 독서 시작
            {3,0,500,0,2}, {3,0,720,2,5}, {3,0,900,6,7}, {3,1,400,3,3}, {3,1,360,4,3}, {3,2,500,5,3},
            // Month -2: 알고리즘 급성장
            {2,0,500,0,2}, {2,0,720,2,5}, {2,0,920,6,8}, {2,1,400,3,3}, {2,1,350,4,3}, {2,2,560,5,5},
            // Month -1: 알고리즘 정점 + 독서 강세
            {1,0,500,0,2}, {1,0,720,2,5}, {1,0,940,6,10}, {1,1,400,3,3}, {1,1,350,4,2}, {1,2,600,5,6},
        };

        // ── 월별 데이터 생성 (WateringLog + TilTag + PointLog 포함) ───
        int[] potExp = {0, 0, 0};
        for (int[] row : plan) {
            int monthsAgo = row[0], potIdx = row[1], charCount = row[2];
            Tag tag = TAGS[row[3]]; Pot pot = POTS[potIdx]; int count = row[4];
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0));
            int point = exp / 10;
            LocalDate base = today.minusMonths(monthsAgo).withDayOfMonth(1);
            for (int i = 0; i < count; i++) {
                LocalDate date = base.plusDays(Math.min(i * 2, 26));
                int beforeExp = potExp[potIdx];
                potExp[potIdx] += exp;
                Til til = tilRepository.save(Til.create(user,
                        tag.getName() + " - " + date, "학습 내용 정리", pot));
                jdbcTemplate.update("UPDATE til SET published_at=? WHERE post_id=?",
                        date.atTime(21, 0), til.getId());
                tilTagRepository.save(TilTag.of(til, tag));
                insertWateringLog(user.getId(), pot.getId(), til.getId(),
                        exp, point, charCount, 0, 1.0, 1, 1, beforeExp, potExp[potIdx], date.atTime(21, 0));
                savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(21, 0));
            }
        }

        // ── 이번 달 TIL (스트릭 + 연속 기록 반영) ─────────────────────
        int curCodingExp = 0, curEnglishExp = 0, curReadingExp = 0;

        // 코딩: 연속 14일 포함, 스트릭 보너스 적용
        int[][] codingDays = {
            {29,600},{27,800},{25,500},{23,700},{21,400},
            {19,900},{17,600},{15,800},{13,500},
            {11,600},{10,700},{9,800},{8,500},{7,900},
            {6,600},{5,700},{4,800},{3,650},{2,750},{1,600},{0,900}
        };
        for (int[] d : codingDays) {
            int daysAgo = d[0], charCount = d[1];
            LocalDate date = today.minusDays(daysAgo);
            int streakDays = daysAgo <= 13 ? (13 - daysAgo) : 0;
            double multiplier = 1.0 + Math.min(streakDays * 0.05, 0.5);
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0) * multiplier);
            int point = exp / 10;
            int before = potExp[0] + curCodingExp; curCodingExp += exp;
            Til til = tilRepository.save(Til.create(user,
                    "코딩 TIL - " + date, "오늘 배운 내용을 정리합니다.".repeat(charCount / 17), codingPot));
            jdbcTemplate.update("UPDATE til SET published_at=? WHERE post_id=?", date.atTime(21, 0), til.getId());
            insertWateringLog(user.getId(), codingPot.getId(), til.getId(),
                    exp, point, charCount, streakDays, multiplier, 1, 1, before, before + exp, date.atTime(21, 0));
            savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(21, 0));
            Tag tag = (daysAgo % 2 == 0) ? javaTag : reactTag;
            tilTagRepository.save(TilTag.of(til, tag));
            if (daysAgo % 3 == 0) tilTagRepository.save(TilTag.of(til, algoTag));
        }

        // 영어
        int[][] englishDays = {
            {28,400},{24,500},{20,350},{16,450},{12,500},
            {8,400},{5,450},{3,500},{1,380},{0,520}
        };
        for (int[] d : englishDays) {
            int daysAgo = d[0], charCount = d[1];
            LocalDate date = today.minusDays(daysAgo);
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0));
            int point = exp / 10;
            int before = potExp[1] + curEnglishExp; curEnglishExp += exp;
            Til til = tilRepository.save(Til.create(user,
                    "영어 TIL - " + date, "영어 학습 내용입니다.".repeat(charCount / 11), englishPot));
            jdbcTemplate.update("UPDATE til SET published_at=? WHERE post_id=?", date.atTime(20, 0), til.getId());
            insertWateringLog(user.getId(), englishPot.getId(), til.getId(),
                    exp, point, charCount, 0, 1.0, 1, 1, before, before + exp, date.atTime(20, 0));
            savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(20, 0));
            tilTagRepository.save(TilTag.of(til, daysAgo % 2 == 0 ? engTag : gramTag));
        }

        // 독서
        int[][] readingDays = {{6,300},{3,350},{0,400}};
        for (int[] d : readingDays) {
            int daysAgo = d[0], charCount = d[1];
            LocalDate date = today.minusDays(daysAgo);
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0));
            int point = exp / 10;
            int before = potExp[2] + curReadingExp; curReadingExp += exp;
            Til til = tilRepository.save(Til.create(user,
                    "독서 TIL - " + date, "책에서 인상 깊었던 구절입니다.".repeat(charCount / 15), readingPot));
            jdbcTemplate.update("UPDATE til SET published_at=? WHERE post_id=?", date.atTime(19, 0), til.getId());
            insertWateringLog(user.getId(), readingPot.getId(), til.getId(),
                    exp, point, charCount, 0, 1.0, 1, 1, before, before + exp, date.atTime(19, 0));
            savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(19, 0));
            tilTagRepository.save(TilTag.of(til, bookTag));
        }

        // ── 임시저장 TIL ───────────────────────────────────────────────
        tilRepository.save(Til.createDraft(user,
                "작성 중인 TIL 초안",
                "아직 정리 중인 내용... React Query의 staleTime 설정에 대해 알아보다가",
                codingPot));

        // ── AI 포인트 소비 이력 ─────────────────────────────────────────
        savePointLogWithSign(user, -50, PointLogReason.AI_SUMMARY, today.minusDays(5).atTime(14, 0));
        savePointLogWithSign(user, -30, PointLogReason.AI_QUIZ,    today.minusDays(3).atTime(16, 0));
        savePointLogWithSign(user, -50, PointLogReason.AI_SUMMARY, today.minusDays(1).atTime(11, 0));

        // ── 화분 exp/level 업데이트 ───────────────────────────────────
        int codingTotal  = potExp[0] + curCodingExp;
        int englishTotal = potExp[1] + curEnglishExp;
        int readingTotal = potExp[2] + curReadingExp;
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                codingTotal,  calcLevel(codingTotal),  codingPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                englishTotal, calcLevel(englishTotal), englishPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                readingTotal, calcLevel(readingTotal), readingPot.getId());

        // ── 유저 최종 포인트 ──────────────────────────────────────────
        int earned = (codingTotal + englishTotal + readingTotal) / 10;
        int used   = 50 + 30 + 50;
        jdbcTemplate.update("UPDATE users SET point=? WHERE id=?", Math.max(earned - used, 0), user.getId());

        log.info("테스트 데이터 생성 완료 — 12개월치, 포인트: {}P", Math.max(earned - used, 0));
    }

    // ── 유틸 ────────────────────────────────────────────────────────────

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name).orElseGet(() -> tagRepository.save(Tag.create(name)));
    }

    private void insertWateringLog(Long userId, Long potId, Long postId,
                                   int exp, int point, int contentLength,
                                   int streakDays, double multiplier,
                                   int beforeLevel, int afterLevel,
                                   int beforeExp, int afterExp,
                                   LocalDateTime wateredAt) {
        jdbcTemplate.update("""
                INSERT INTO watering_log
                (user_id, pot_id, post_id, exp_gained, point_gained, content_length,
                 streak_days, applied_multiplier, before_pot_level, after_pot_level,
                 before_total_exp, after_total_exp, watered_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId, potId, postId, exp, point, contentLength,
                streakDays, multiplier, beforeLevel, afterLevel,
                beforeExp, afterExp, wateredAt);
    }

    private void savePointLog(User user, int amount, PointLogReason reason, LocalDateTime createdAt) {
        if (amount <= 0) return;
        savePointLogWithSign(user, amount, reason, createdAt);
    }

    private void savePointLogWithSign(User user, int amount, PointLogReason reason, LocalDateTime createdAt) {
        if (amount == 0) return;
        PointLog saved = pointLogRepository.save(PointLog.builder()
                .user(user).reason(reason).amount(amount).build());
        jdbcTemplate.update("UPDATE point_log SET created_at=? WHERE id=?", createdAt, saved.getId());
    }

    private int calcLevel(int totalExp) {
        int level = 1, remaining = totalExp;
        while (remaining >= level * 100) { remaining -= level * 100; level++; }
        return level;
    }
}
