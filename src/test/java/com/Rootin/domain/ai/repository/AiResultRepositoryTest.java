package com.Rootin.domain.ai.repository;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.annotation.H2RepositoryTest;
import com.Rootin.global.annotation.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class AiResultRepositoryTest {

    @Autowired
    AiResultRepository aiResultRepository;

    @Autowired
    EntityManager em;

    private User user;
    private User otherUser;
    private Pot pot;
    private Pot otherPot;
    private Til til;
    private Til otherTil;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        user = new User();
        ReflectionTestUtils.setField(user, "email", "owner@test.com");
        em.persist(user);

        otherUser = new User();
        ReflectionTestUtils.setField(otherUser, "email", "other@test.com");
        em.persist(otherUser);

        em.flush(); // user.getId() 확보

        // 화분 생성 (userId는 flush 후 확보된 id 사용)
        pot = Pot.builder()
                .userId(user.getId())
                .title("내 화분")
                .level(1)
                .totalExp(0)
                .build();
        em.persist(pot);

        otherPot = Pot.builder()
                .userId(otherUser.getId())
                .title("타인 화분")
                .level(1)
                .totalExp(0)
                .build();
        em.persist(otherPot);

        em.flush(); // pot.getId() 확보

        // TIL 생성 (JOINED 상속: posts + til 테이블 동시 저장)
        til = Til.create(user, "TIL 제목", "TIL 내용", pot);
        em.persist(til);

        otherTil = Til.create(otherUser, "타인 TIL", "타인 내용", otherPot);
        em.persist(otherTil);

        em.flush();
    }

    // ─── 저장 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("SUMMARY 타입 AiResult 저장 — ai_result_til 연결 확인")
    void save_and_find_summary() {
        AiResult aiResult = AiResult.builder()
                .user(user)
                .resultContent("TIL 핵심 요약입니다.")
                .toolType(ToolType.SUMMARY)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        saved.addTil(til);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getToolType()).isEqualTo(ToolType.SUMMARY);
        assertThat(found.get().getResultContent()).isEqualTo("TIL 핵심 요약입니다.");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getTils()).hasSize(1);
    }

    @Test
    @DisplayName("SUMMARY 저장 시 count, difficulty는 null")
    void summary_has_null_count_and_difficulty() {
        AiResult aiResult = AiResult.builder()
                .user(user)
                .resultContent("요약 내용")
                .toolType(ToolType.SUMMARY)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        saved.addTil(til);
        em.flush();
        em.clear();

        AiResult found = aiResultRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCount()).isNull();
        assertThat(found.getDifficulty()).isNull();
    }

    // ─── 조회 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByUser - 본인 결과만 반환, 타인 결과 제외")
    void findAllByUser_returns_only_owner_results() {
        AiResult r1 = aiResultRepository.save(AiResult.builder()
                .user(user)
                .resultContent("내 요약").toolType(ToolType.SUMMARY).build());
        r1.addTil(til);

        AiResult r2 = aiResultRepository.save(AiResult.builder()
                .user(otherUser)
                .resultContent("타인 요약").toolType(ToolType.SUMMARY).build());
        r2.addTil(otherTil);

        em.flush();
        em.clear();

        List<AiResult> results = aiResultRepository.findAllByUser(user);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultContent()).isEqualTo("내 요약");
    }

    @Test
    @DisplayName("findAllByUserAndPotId - 화분 기준 필터링")
    void findAllByUserAndPotId_returns_filtered_results() {
        // 내 화분 결과
        AiResult r1 = aiResultRepository.save(AiResult.builder()
                .user(user)
                .resultContent("내 화분 요약").toolType(ToolType.SUMMARY).build());
        r1.addTil(til);

        // 다른 화분의 TIL로 저장된 결과
        Pot pot2 = Pot.builder().userId(user.getId()).title("두번째 화분").level(1).totalExp(0).build();
        em.persist(pot2);
        em.flush();
        Til til2 = Til.create(user, "TIL2", "두번째 내용", pot2);
        em.persist(til2);
        em.flush();

        AiResult r2 = aiResultRepository.save(AiResult.builder()
                .user(user)
                .resultContent("다른 화분 요약").toolType(ToolType.SUMMARY).build());
        r2.addTil(til2);

        em.flush();
        em.clear();

        List<AiResult> results = aiResultRepository.findAllByUserAndPotId(user, pot.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultContent()).isEqualTo("내 화분 요약");
    }

    @Test
    @DisplayName("ai_result_til 중간 테이블 — 여러 TIL 연결 확인")
    void multiple_tils_linked_in_join_table() {
        Til til2 = Til.create(user, "TIL2", "두 번째 TIL 내용", pot);
        em.persist(til2);
        em.flush();

        AiResult aiResult = AiResult.builder()
                .user(user)
                .resultContent("두 TIL 요약")
                .toolType(ToolType.SUMMARY)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        saved.addTil(til);
        saved.addTil(til2);
        em.flush();
        em.clear();

        AiResult found = aiResultRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTils()).hasSize(2);
    }
}
