package com.Rootin.domain.ai.repository;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.Difficulty;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.annotation.H2RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

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
        user = new User();
        ReflectionTestUtils.setField(user, "email", "test@test.com");
        em.persist(user);

        post = new Post();
        ReflectionTestUtils.setField(post, "user", user);
        em.persist(post);

        em.flush();
    }

    @Test
    @DisplayName("SUMMARY 타입 AiResult 저장 및 조회")
    void save_and_find_summary() {
        AiResult aiResult = AiResult.builder()
                .post(post).user(user)
                .resultContent("TIL 핵심 요약입니다.")
                .toolType(ToolType.SUMMARY)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getToolType()).isEqualTo(ToolType.SUMMARY);
        assertThat(found.get().getResultContent()).isEqualTo("TIL 핵심 요약입니다.");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("QUIZ 타입 AiResult 저장 및 조회 - count, difficulty 포함")
    void save_and_find_quiz() {
        AiResult aiResult = AiResult.builder()
                .post(post).user(user)
                .resultContent("문제 1번: ...")
                .toolType(ToolType.QUIZ)
                .count(5)
                .difficulty(Difficulty.HIGH)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCount()).isEqualTo(5);
        assertThat(found.get().getDifficulty()).isEqualTo(Difficulty.HIGH);
    }

    @Test
    @DisplayName("SUMMARY 저장 시 count, difficulty는 null")
    void summary_has_null_count_and_difficulty() {
        AiResult aiResult = AiResult.builder()
                .post(post).user(user)
                .resultContent("요약 내용")
                .toolType(ToolType.SUMMARY)
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        em.flush();
        em.clear();

        Optional<AiResult> found = aiResultRepository.findById(saved.getId());

        assertThat(found.get().getCount()).isNull();
        assertThat(found.get().getDifficulty()).isNull();
    }
}
