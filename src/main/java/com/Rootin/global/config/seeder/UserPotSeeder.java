package com.Rootin.global.config.seeder;

import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPotSeeder {

    static final String TEST_EMAIL = "test@rootin.com";

    private final UserRepository userRepository;
    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 유저·화분·PlantItem 생성.
     * 이미 존재하면 스킵하고 empty 반환, 생성했으면 SeedContext 반환.
     */
    public Optional<SeedContext> seed() {
        if (userRepository.findByEmail(TEST_EMAIL).isPresent()) {
            log.info("테스트 유저 이미 존재. 스킵.");
            return Optional.empty();
        }

        User user = userRepository.save(User.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode("test1234"))
                .nickname("루틴이")
                .bio("루틴처럼 기록하고, 뿌리처럼 깊어지는 중.")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .point(0)
                .build());

        // ── 식물 마스터 조회 ───────────────────────────────────────────────────
        Plant seedPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .orElseThrow();
        Plant moonPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("달빛씨앗", Grade.RARE, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 달빛씨앗 (RARE/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant shroomPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("버섯씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 버섯씨앗 (COMMON/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant cactusPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("선인장씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 선인장씨앗 (COMMON/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant boltPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("번개씨앗", Grade.RARE, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 번개씨앗 (RARE/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant firePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("불꽃씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 불꽃씨앗 (COMMON/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant icePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("얼음씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 얼음씨앗 (COMMON/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));
        Plant rosePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("흑장미씨앗", Grade.RARE, GrowthStage.SEED)
                .orElseThrow(() -> new IllegalStateException("식물 마스터 데이터 누락: 흑장미씨앗 (RARE/SEED). PlantMasterSeeder가 먼저 실행되었는지 확인하세요."));

        // ── plant_collection: 8종 씨앗 전부 해금 ────────────────────────────────
        for (Plant seed : new Plant[]{seedPlant, moonPlant, shroomPlant, cactusPlant,
                boltPlant, firePlant, icePlant, rosePlant}) {
            jdbcTemplate.update(
                    "INSERT IGNORE INTO plant_collection (user_id, plant_id, created_at) VALUES (?, ?, NOW())",
                    user.getId(), seed.getId());
        }

        // ── 화분 생성 ──────────────────────────────────────────────────────────
        Pot codingPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("코딩").description("매일 한 가지씩 배우는 코딩 기록")
                .level(1).totalExp(0).isDisplayed(true).build());
        Pot englishPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("영어").description("영어 학습 기록")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot readingPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("독서").description("책에서 건진 문장과 생각")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot mathPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("수학").description("수학 개념과 풀이 기록")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot fitnessPot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("운동").description("운동 루틴과 기록")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot firePot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("요리").description("요리 레시피와 도전 기록")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot icePot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("명상").description("매일 5분 마음 정리")
                .level(1).totalExp(0).isDisplayed(false).build());
        Pot rosePot = potRepository.save(Pot.builder()
                .userId(user.getId()).title("그림").description("그림 연습 기록")
                .level(1).totalExp(0).isDisplayed(false).build());

        LocalDate today = LocalDate.now();

        // ── 도감 해금용 수확 이력 ──────────────────────────────────────────────
        // CollectionService 로직: harvestedStageIndex 값으로 해당 stage 점등

        // [seed] 기본 씨앗 3회 수확 → 씨드몬(0), 새싹몬(1), 잎몬(2) 해금
        saveHarvested(user, codingPot, seedPlant, 0,
                today.minusMonths(12).atTime(10, 0),
                today.minusMonths(9).atTime(20, 0));
        saveHarvested(user, codingPot, seedPlant, 1,
                today.minusMonths(9).atTime(20, 1),
                today.minusMonths(5).atTime(20, 0));
        saveHarvested(user, codingPot, seedPlant, 2,
                today.minusMonths(5).atTime(20, 1),
                today.minusMonths(2).atTime(20, 0));

        // [shroom] 버섯씨앗 2회 수확 → 포자씨(0), 애버섯(1) 해금
        saveHarvested(user, readingPot, shroomPlant, 0,
                today.minusMonths(10).atTime(10, 0),
                today.minusMonths(7).atTime(20, 0));
        saveHarvested(user, readingPot, shroomPlant, 1,
                today.minusMonths(7).atTime(20, 1),
                today.minusMonths(3).atTime(20, 0));

        // [cactus] 선인장씨앗 1회 수확 → 가시씨(0) 해금
        saveHarvested(user, mathPot, cactusPlant, 0,
                today.minusMonths(8).atTime(10, 0),
                today.minusMonths(4).atTime(20, 0));

        // [moon] 달빛씨앗 2회 수확 → 달빛씨(0), 달빛싹(1) 해금 (희귀종)
        saveHarvested(user, englishPot, moonPlant, 0,
                today.minusMonths(11).atTime(10, 0),
                today.minusMonths(8).atTime(20, 0));
        saveHarvested(user, englishPot, moonPlant, 1,
                today.minusMonths(8).atTime(20, 1),
                today.minusMonths(4).atTime(20, 0));

        // [bolt] 번개씨앗 1회 수확 → 번개씨(0) 해금 (희귀종)
        saveHarvested(user, fitnessPot, boltPlant, 0,
                today.minusMonths(6).atTime(10, 0),
                today.minusMonths(3).atTime(20, 0));

        // [fire] 불꽃씨앗 1회 수확 → 불꽃씨(0) 해금
        saveHarvested(user, firePot, firePlant, 0,
                today.minusMonths(7).atTime(10, 0),
                today.minusMonths(4).atTime(20, 0));

        // [ice] 얼음씨앗 2회 수확 → 얼음씨(0), 얼음싹(1) 해금
        saveHarvested(user, icePot, icePlant, 0,
                today.minusMonths(9).atTime(10, 0),
                today.minusMonths(6).atTime(20, 0));
        saveHarvested(user, icePot, icePlant, 1,
                today.minusMonths(6).atTime(20, 1),
                today.minusMonths(2).atTime(20, 0));

        // [rose] 흑장미씨앗 1회 수확 → 흑장미씨(0) 해금 (희귀종)
        saveHarvested(user, rosePot, rosePlant, 0,
                today.minusMonths(5).atTime(10, 0),
                today.minusMonths(1).atTime(20, 0));

        // ── 현재 성장 중인 PlantItem ───────────────────────────────────────────

        // seed 4번째 사이클: 새싹 성장 중
        PlantItem codingActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(seedPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(2).atTime(20, 1), 250, codingActive.getId());

        // moon 3번째 사이클: 개화 직전 성장 중
        PlantItem englishActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(englishPot.getId()).plantId(moonPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(4).atTime(20, 1), 850, englishActive.getId());

        // shroom 3번째 사이클: 씨앗 단계
        PlantItem readingActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(readingPot.getId()).plantId(shroomPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(3).atTime(20, 1), 80, readingActive.getId());

        // cactus 2번째 사이클: 씨앗 단계
        PlantItem mathActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(mathPot.getId()).plantId(cactusPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(4).atTime(20, 1), 150, mathActive.getId());

        // bolt 2번째 사이클: 씨앗 단계
        PlantItem fitnessActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(fitnessPot.getId()).plantId(boltPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(3).atTime(20, 1), 50, fitnessActive.getId());

        // fire 2번째 사이클: 새싹 성장 중
        PlantItem fireActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(firePot.getId()).plantId(firePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(4).atTime(20, 1), 200, fireActive.getId());

        // ice 3번째 사이클: 씨앗 단계
        PlantItem iceActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(icePot.getId()).plantId(icePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(2).atTime(20, 1), 30, iceActive.getId());

        // rose 2번째 사이클: 씨앗 단계 (희귀종)
        PlantItem roseActive = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(rosePot.getId()).plantId(rosePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(1).atTime(20, 1), 10, roseActive.getId());

        log.info("유저·화분·PlantItem 생성 완료 — 도감 13/40 해금 (seed×3, shroom×2, cactus×1, moon×2, bolt×1, fire×1, ice×2, rose×1)");
        return Optional.of(new SeedContext(user, codingPot, englishPot, readingPot, mathPot, fitnessPot, firePot, icePot, rosePot));
    }

    private void saveHarvested(User user, Pot pot, Plant plant, int stageIndex,
                               LocalDateTime createdAt, LocalDateTime harvestedAt) {
        PlantItem item = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(pot.getId()).plantId(plant.getId())
                .isHarvested(true).harvestedLevel(10).growthExp(1000).harvestedStageIndex(stageIndex).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, harvested_at=? WHERE id=?",
                createdAt, harvestedAt, item.getId());
    }

    /** TilSeeder로 전달할 컨텍스트 */
    public record SeedContext(User user, Pot codingPot, Pot englishPot, Pot readingPot, Pot mathPot, Pot fitnessPot,
                              Pot firePot, Pot icePot, Pot rosePot) {}
}
