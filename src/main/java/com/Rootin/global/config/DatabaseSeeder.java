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
import java.util.List;

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

    // ── 1. 식물 마스터 데이터 ────────────────────────────────────
    private void seedPlantMaster() {
        boolean exists = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .isPresent();
        if (!exists) {
            plantRepository.save(Plant.builder()
                    .name(DEFAULT_PLANT_NAME).grade(Grade.COMMON).growthStage(GrowthStage.SEED)
                    .imageUrl(null).silhouetteUrl(null).build());

            plantRepository.save(Plant.builder()
                    .name("달빛씨앗").grade(Grade.RARE).growthStage(GrowthStage.SEED)
                    .imageUrl(null).silhouetteUrl(null).build());

            log.info("식물 마스터 데이터 저장 완료");
        }
    }

    // ── 2. 테스트 데이터 (local 환경 전용) ──────────────────────
    private void seedTestData() {
        if (userRepository.findByEmail(TEST_EMAIL).isPresent()) {
            log.info("테스트 데이터 이미 존재. 스킵.");
            return;
        }

        log.info("테스트 데이터 생성 시작...");

        // 2-1. 유저
        User user = userRepository.save(User.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode("test1234"))
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .point(0)
                .build());

        // 2-2. 화분 + 식물 아이템
        Plant defaultPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .orElseThrow();
        Plant rarePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("달빛씨앗", Grade.RARE, GrowthStage.SEED)
                .orElse(defaultPlant);

        Pot codingPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("코딩").description("매일 한 가지씩 배우는 코딩 기록")
                .level(1).totalExp(0).isDisplayed(true).build());

        // 코딩 화분: 이전 라운드 수확 이력 (식물도감에 수집됨으로 표시)
        PlantItem prevCodingHarvested = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId())
                .isHarvested(true).harvestedLevel(8).growthExp(1000).build());
        jdbcTemplate.update("UPDATE plant_item SET harvested_at = ? WHERE id = ?",
                LocalDateTime.now().minusMonths(1), prevCodingHarvested.getId());

        // 코딩 화분: 현재 라운드 — SPROUT (2단계)
        PlantItem codingPlantItem = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET growth_exp = ? WHERE id = ?", 250, codingPlantItem.getId());

        Pot englishPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("영어").description("영어 학습 기록")
                .level(1).totalExp(0).isDisplayed(false).build());

        // 영어 화분: 첫 라운드 — BLOOM (4단계)
        PlantItem englishPlantItem = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(englishPot.getId()).plantId(rarePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET growth_exp = ? WHERE id = ?", 850, englishPlantItem.getId());

        // 2-3. 태그
        Tag javaTag    = getOrCreateTag("Java");
        Tag springTag  = getOrCreateTag("Spring");
        Tag reactTag   = getOrCreateTag("React");
        Tag englishTag = getOrCreateTag("영어");
        Tag grammarTag = getOrCreateTag("문법");

        // 2-4. TIL + WateringLog (지난 30일)
        LocalDate today = LocalDate.now();
        int totalCodingExp  = 0;
        int totalEnglishExp = 0;

        // 코딩 화분: 지난 20일 (연속 12일 포함)
        int[][] codingDays = {
            {29,600},{27,800},{25,500},{23,700},{21,400},
            {19,900},{17,600},{15,800},{13,500},
            // 연속 12일
            {11,600},{10,700},{9,800},{8,500},{7,900},
            {6,600},{5,700},{4,800},{3,650},{2,750},{1,600},{0,900}
        };

        for (int[] d : codingDays) {
            int daysAgo = d[0];
            int charCount = d[1];
            LocalDate date = today.minusDays(daysAgo);
            int streakDays = daysAgo <= 11 ? (11 - daysAgo) : 0;
            double multiplier = 1.0 + Math.min(streakDays * 0.05, 0.5);
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0) * multiplier);
            int point = exp / 10;

            int beforeExp = totalCodingExp;
            totalCodingExp += exp;

            Til til = tilRepository.save(Til.create(user,
                    "코딩 TIL - " + date,
                    "오늘 배운 내용을 정리합니다. ".repeat(charCount / 15),
                    codingPot));

            // publishedAt 과거 날짜로 업데이트
            jdbcTemplate.update("UPDATE til SET published_at = ? WHERE post_id = ?",
                    date.atTime(21, 0), til.getId());

            // WateringLog 직접 삽입
            insertWateringLog(user.getId(), codingPot.getId(), til.getId(),
                    exp, point, charCount, streakDays, multiplier,
                    1, 1, beforeExp, totalCodingExp, date.atTime(21, 0));

            // 포인트 로그
            savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(21, 0));

            // 태그 (번갈아가며)
            Tag tag = (daysAgo % 2 == 0) ? javaTag : springTag;
            tilTagRepository.save(TilTag.of(til, tag));
            if (daysAgo % 3 == 0) tilTagRepository.save(TilTag.of(til, reactTag));
        }

        // 영어 화분: 지난 15일
        int[][] englishDays = {
            {28,400},{24,500},{20,350},{16,450},{12,500},
            {8,400},{5,450},{3,500},{1,380},{0,520}
        };

        for (int[] d : englishDays) {
            int daysAgo = d[0];
            int charCount = d[1];
            LocalDate date = today.minusDays(daysAgo);
            int exp = (int) Math.floor(Math.min(charCount * 0.2, 300.0));
            int point = exp / 10;

            int beforeExp = totalEnglishExp;
            totalEnglishExp += exp;

            Til til = tilRepository.save(Til.create(user,
                    "영어 TIL - " + date,
                    "영어 학습 내용입니다. ".repeat(charCount / 10),
                    englishPot));

            jdbcTemplate.update("UPDATE til SET published_at = ? WHERE post_id = ?",
                    date.atTime(20, 0), til.getId());

            insertWateringLog(user.getId(), englishPot.getId(), til.getId(),
                    exp, point, charCount, 0, 1.0,
                    1, 1, beforeExp, totalEnglishExp, date.atTime(20, 0));

            savePointLog(user, point, PointLogReason.TIL_WRITE, date.atTime(20, 0));

            Tag tag = (daysAgo % 2 == 0) ? englishTag : grammarTag;
            tilTagRepository.save(TilTag.of(til, tag));
        }

        // 화분 경험치/레벨 업데이트
        jdbcTemplate.update("UPDATE pot SET total_exp = ?, level = ? WHERE id = ?",
                totalCodingExp, calcLevel(totalCodingExp), codingPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp = ?, level = ? WHERE id = ?",
                totalEnglishExp, calcLevel(totalEnglishExp), englishPot.getId());

        // 유저 포인트 합산
        int totalPoint = totalCodingExp / 10 + totalEnglishExp / 10;
        jdbcTemplate.update("UPDATE users SET point = ? WHERE id = ?", totalPoint, user.getId());

        log.info("테스트 데이터 생성 완료 — 유저: {}, 코딩Exp: {}, 영어Exp: {}, 포인트: {}",
                TEST_EMAIL, totalCodingExp, totalEnglishExp, totalPoint);
    }

    // ── 유틸 ────────────────────────────────────────────────────

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
        PointLog log = PointLog.builder()
                .user(user).reason(reason).amount(amount).build();
        PointLog saved = pointLogRepository.save(log);
        jdbcTemplate.update("UPDATE point_log SET created_at = ? WHERE id = ?", createdAt, saved.getId());
    }

    private int calcLevel(int totalExp) {
        int level = 1;
        int remaining = totalExp;
        while (remaining >= level * 100) {
            remaining -= level * 100;
            level++;
        }
        return level;
    }
}
