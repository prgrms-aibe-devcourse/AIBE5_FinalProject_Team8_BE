package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.Difficulty;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.ai.dto.AiResultResponse;
import com.Rootin.domain.ai.dto.AiResultSaveRequest;
import com.Rootin.domain.ai.repository.AiResultRepository;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.til.repository.PostRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiResultServiceTest {

    @InjectMocks
    private AiResultService aiResultService;

    @Mock
    private AiResultRepository aiResultRepository;

    @Mock
    private PostRepository postRepository;

    private User owner;
    private User other;
    private Post post;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);

        other = new User();
        ReflectionTestUtils.setField(other, "id", 2L);

        post = new Post();
        ReflectionTestUtils.setField(post, "id", 10L);
        ReflectionTestUtils.setField(post, "user", owner);
    }

    // ─── save() 테스트 ───────────────────────────────────────────────

    @Test
    @DisplayName("SUMMARY 타입 - 정상 저장")
    void save_summary_success() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.SUMMARY, 10L, "요약 내용", null, null
        );

        AiResult savedResult = AiResult.builder()
                .post(post).user(owner)
                .resultContent("요약 내용")
                .toolType(ToolType.SUMMARY)
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 100L);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(aiResultRepository.save(any())).willReturn(savedResult);

        AiResultResponse response = aiResultService.save(request, owner);

        assertThat(response.type()).isEqualTo(ToolType.SUMMARY);
        assertThat(response.content()).isEqualTo("요약 내용");
    }

    @Test
    @DisplayName("QUIZ 타입 - 정상 저장")
    void save_quiz_success() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.QUIZ, 10L, "문제 내용", Difficulty.HIGH, 5
        );

        AiResult savedResult = AiResult.builder()
                .post(post).user(owner)
                .resultContent("문제 내용")
                .toolType(ToolType.QUIZ)
                .difficulty(Difficulty.HIGH)
                .count(5)
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 101L);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(aiResultRepository.save(any())).willReturn(savedResult);

        AiResultResponse response = aiResultService.save(request, owner);

        assertThat(response.type()).isEqualTo(ToolType.QUIZ);
    }

    @Test
    @DisplayName("타인 TIL에 저장 시도 → 403 예외")
    void save_forbidden_when_not_owner() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.SUMMARY, 10L, "요약 내용", null, null
        );

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> aiResultService.save(request, other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 TIL → 404 예외")
    void save_notFound_when_post_not_exists() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.SUMMARY, 999L, "요약 내용", null, null
        );

        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("QUIZ 타입에 difficulty 없으면 → 400 예외")
    void save_badRequest_when_quiz_without_difficulty() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.QUIZ, 10L, "문제 내용", null, 5
        );

        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("QUIZ 타입에 count가 0 이하면 → 400 예외")
    void save_badRequest_when_quiz_count_zero() {
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.QUIZ, 10L, "문제 내용", Difficulty.MEDIUM, 0
        );

        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ─── getResults() 테스트 ─────────────────────────────────────────

    @Test
    @DisplayName("tilId 없이 조회 → 본인 전체 결과 반환")
    void getResults_without_tilId_returns_all() {
        AiResult result1 = AiResult.builder()
                .post(post).user(owner).resultContent("요약1").toolType(ToolType.SUMMARY).build();
        AiResult result2 = AiResult.builder()
                .post(post).user(owner).resultContent("문제1").toolType(ToolType.QUIZ)
                .difficulty(Difficulty.HIGH).count(3).build();
        ReflectionTestUtils.setField(result1, "id", 1L);
        ReflectionTestUtils.setField(result2, "id", 2L);

        given(aiResultRepository.findAllByUser(owner)).willReturn(List.of(result1, result2));

        List<AiResultResponse> responses = aiResultService.getResults(owner, null);

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("tilId로 필터링 → 해당 TIL 결과만 반환")
    void getResults_with_tilId_returns_filtered() {
        AiResult result = AiResult.builder()
                .post(post).user(owner).resultContent("요약").toolType(ToolType.SUMMARY).build();
        ReflectionTestUtils.setField(result, "id", 1L);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(aiResultRepository.findAllByUserAndPost(owner, post)).willReturn(List.of(result));

        List<AiResultResponse> responses = aiResultService.getResults(owner, 10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).type()).isEqualTo(ToolType.SUMMARY);
    }

    @Test
    @DisplayName("타인 TIL로 조회 시도 → 403 예외")
    void getResults_forbidden_when_not_owner_tilId() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> aiResultService.getResults(other, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 tilId로 조회 → 404 예외")
    void getResults_notFound_when_post_not_exists() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiResultService.getResults(owner, 999L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
