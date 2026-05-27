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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2RepositoryTest
class AiResultRepositoryTest {

    @Autowired
    AiResultRepository aiResultRepository;

    @Autowired
    EntityManager em;

    private User user;
    private User otherUser;
    private Post post;
    private Post otherPost;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "email", "owner@test.com");
        em.persist(user);

        otherUser = new User();
        ReflectionTestUtils.setField(otherUser, "email", "other@test.com");
        em.persist(otherUser);

        post = new Post();
        ReflectionTestUtils.setField(post, "user", user);
        em.persist(post);

        otherPost = new Post();
        ReflectionTestUtils.setField(otherPost, "user", otherUser);
        em.persist(otherPost);

        em.flush();
    }

    // ─── 저장 테스트 ────────────────────────────────────────────────

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

    // ─── 조회 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByUser - 본인 결과만 반환, 타인 결과 제외")
    void findAllByUser_returns_only_owner_results() {
        aiResultRepository.save(AiResult.builder()
                .post(post).user(user).resultContent("내 요약").toolType(ToolType.SUMMARY).build());
        aiResultRepository.save(AiResult.builder()
                .post(otherPost).user(otherUser).resultContent("타인 요약").toolType(ToolType.SUMMARY).build());
        em.flush();
        em.clear();

        List<AiResult> results = aiResultRepository.findAllByUser(user);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultContent()).isEqualTo("내 요약");
    }

    @Test
    @DisplayName("findAllByUserAndPost - 특정 TIL 결과만 반환")
    void findAllByUserAndPost_returns_filtered_results() {
        Post anotherPost = new Post();
        ReflectionTestUtils.setField(anotherPost, "user", user);
        em.persist(anotherPost);
        em.flush();

        aiResultRepository.save(AiResult.builder()
                .post(post).user(user).resultContent("TIL1 요약").toolType(ToolType.SUMMARY).build());
        aiResultRepository.save(AiResult.builder()
                .post(anotherPost).user(user).resultContent("TIL2 요약").toolType(ToolType.SUMMARY).build());
        em.flush();
        em.clear();

        List<AiResult> results = aiResultRepository.findAllByUserAndPost(user, post);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultContent()).isEqualTo("TIL1 요약");
    }
}
