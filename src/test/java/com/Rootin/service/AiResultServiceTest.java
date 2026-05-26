package com.Rootin.service;

import com.Rootin.domain.AiResult;
import com.Rootin.domain.Post;
import com.Rootin.domain.User;
import com.Rootin.domain.enums.Difficulty;
import com.Rootin.domain.enums.ToolType;
import com.Rootin.dto.AiResultResponse;
import com.Rootin.dto.AiResultSaveRequest;
import com.Rootin.exception.CustomException;
import com.Rootin.repository.AiResultRepository;
import com.Rootin.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("SUMMARY 타입 - 정상 저장")
    void save_summary_success() {
        // given
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

        // when
        AiResultResponse response = aiResultService.save(request, owner);

        // then
        assertThat(response.type()).isEqualTo(ToolType.SUMMARY);
        assertThat(response.content()).isEqualTo("요약 내용");
    }

    @Test
    @DisplayName("QUIZ 타입 - 정상 저장")
    void save_quiz_success() {
        // given
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

        // when
        AiResultResponse response = aiResultService.save(request, owner);

        // then
        assertThat(response.type()).isEqualTo(ToolType.QUIZ);
    }

    @Test
    @DisplayName("타인 TIL에 저장 시도 → 403 예외")
    void save_forbidden_when_not_owner() {
        // given
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.SUMMARY, 10L, "요약 내용", null, null
        );

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> aiResultService.save(request, other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("존재하지 않는 TIL → 404 예외")
    void save_notFound_when_post_not_exists() {
        // given
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.SUMMARY, 999L, "요약 내용", null, null
        );

        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("QUIZ 타입에 difficulty 없으면 → 400 예외")
    void save_badRequest_when_quiz_without_difficulty() {
        // given
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.QUIZ, 10L, "문제 내용", null, 5
        );

        // when & then
        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("QUIZ 타입에 count가 0 이하면 → 400 예외")
    void save_badRequest_when_quiz_count_zero() {
        // given
        AiResultSaveRequest request = new AiResultSaveRequest(
                ToolType.QUIZ, 10L, "문제 내용", Difficulty.MEDIUM, 0
        );

        // when & then
        assertThatThrownBy(() -> aiResultService.save(request, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
