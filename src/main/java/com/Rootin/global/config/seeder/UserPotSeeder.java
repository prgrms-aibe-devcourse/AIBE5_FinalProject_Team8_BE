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

        Plant defaultPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage(PlantMasterSeeder.DEFAULT_PLANT_NAME, Grade.COMMON, GrowthStage.SEED)
                .orElseThrow();
        Plant rarePlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("달빛씨앗", Grade.RARE, GrowthStage.SEED)
                .orElse(defaultPlant);
        Plant mushroomPlant = plantRepository
                .findFirstByNameAndGradeAndGrowthStage("버섯씨앗", Grade.COMMON, GrowthStage.SEED)
                .orElse(defaultPlant);

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

        // 코딩 라운드1: 12달 전 ~ 6달 전 (수확 완료)
        PlantItem coding1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId())
                .isHarvested(true).harvestedLevel(10).growthExp(1000).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, harvested_at=? WHERE id=?",
                today.minusMonths(12).atTime(10, 0),
                today.minusMonths(6).atTime(20, 0), coding1.getId());

        // 코딩 라운드2: 6달 전 ~ 현재 (새싹 성장 중)
        PlantItem coding2 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(codingPot.getId()).plantId(defaultPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(6).atTime(20, 1), 250, coding2.getId());

        // 영어: 12달 전 ~ 현재 (개화 성장 중)
        PlantItem english1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(englishPot.getId()).plantId(rarePlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(12).atTime(10, 0), 850, english1.getId());

        // 독서: 3달 전 ~ 현재 (씨앗 단계)
        PlantItem reading1 = plantItemRepository.save(PlantItem.builder()
                .userId(user.getId()).potId(readingPot.getId()).plantId(mushroomPlant.getId()).build());
        jdbcTemplate.update("UPDATE plant_item SET created_at=?, growth_exp=? WHERE id=?",
                today.minusMonths(3).atTime(10, 0), 80, reading1.getId());

        log.info("유저·화분·PlantItem 생성 완료");
        return Optional.of(new SeedContext(user, codingPot, englishPot, readingPot));
    }

    /** TilSeeder로 전달할 컨텍스트 */
    public record SeedContext(User user, Pot codingPot, Pot englishPot, Pot readingPot) {}
}
