package com.Rootin.repository;

import com.Rootin.annotation.H2RepositoryTest;
import com.Rootin.domain.AiResult;
import com.Rootin.domain.Post;
import com.Rootin.domain.User;
import com.Rootin.domain.enums.Difficulty;
import com.Rootin.domain.enums.ToolType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2RepositoryTest
class AiResultRepositoryTest {

    @Autowired
    AiResultRepository aiResultRepository;

    @Autowired
    EntityManager em;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        // User, Post는 스텁 엔티티라 직접 persist
        user = new User();
        // email은 not null 제약 → 더미값 주입
        org.springframework.test.util.ReflectionTestUtils.setField(user, "email", "test@test.com");
        em.persist(user);

        post = new Post();
        // Post의 user 필드 설정 (리플렉션으로 주입)
        org.springframework.test.util.ReflectionTestUtils.setField(post, "user", user);
        em.persist(post);

        em.flush();
    }

    @Test
    @DisplayName("SUMMARY 타입 AiResult 저장 및 조회")
    void save_and_find_summary() {
        // given
        AiResult aiResult = AiResult.builder()
                .post(post)
                .user(user)
                .resultContent("TIL 핵심 요약입니다.")
                .toolType(ToolType.SUMMARY)
                .build();

        // when
        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getToolType()).isEqualTo(ToolType.SUMMARY);
        assertThat(found.get().getResultContent()).isEqualTo("TIL 핵심 요약입니다.");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("QUIZ 타입 AiResult 저장 및 조회 - count, difficulty 포함")
    void save_and_find_quiz() {
        // given
        AiResult aiResult = AiResult.builder()
                .post(post)
                .user(user)
                .resultContent("문제 1번: ...")
                .toolType(ToolType.QUIZ)
                .count(5)
                .difficulty(Difficulty.HIGH)
                .build();

        // when
        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCount()).isEqualTo(5);
        assertThat(found.get().getDifficulty()).isEqualTo(Difficulty.HIGH);
    }

    @Test
    @DisplayName("SUMMARY 저장 시 count, difficulty는 null")
    void summary_has_null_count_and_difficulty() {
        // given
        AiResult aiResult = AiResult.builder()
                .post(post)
                .user(user)
                .resultContent("요약 내용")
                .toolType(ToolType.SUMMARY)
                .build();

        // when
        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        // then
        assertThat(found.get().getCount()).isNull();
        assertThat(found.get().getDifficulty()).isNull();
    }
}
