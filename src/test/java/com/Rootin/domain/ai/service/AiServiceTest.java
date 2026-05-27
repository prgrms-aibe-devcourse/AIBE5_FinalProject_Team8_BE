package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.client.AiPromptClient;
import com.Rootin.domain.ai.constant.AiPolicy;
import com.Rootin.domain.ai.dto.AiQuizItem;
import com.Rootin.domain.ai.dto.AiQuizRequest;
import com.Rootin.domain.ai.dto.AiQuizResponse;
import com.Rootin.domain.ai.dto.AiSummaryRequest;
import com.Rootin.domain.ai.dto.AiSummaryResponse;
import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.til.repository.PostRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @InjectMocks
    private AiService aiService;

    @Mock
    private AiPromptClient aiPromptClient;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private User owner;
    private User other;
    private Post post;

    private static final String MOCK_SUMMARY_JSON =
            "{\"summary\":\"핵심 요약 내용\",\"keyPoints\":[\"포인트1\",\"포인트2\",\"포인트3\"]}";

    private static final String MOCK_QUIZ_JSON =
            "{\"quizzes\":[{\"question\":\"질문1\",\"answer\":\"정답1\",\"hint\":\"힌트1\"}," +
            "{\"question\":\"질문2\",\"answer\":\"정답2\",\"hint\":\"힌트2\"}]}";

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);
        ReflectionTestUtils.setField(owner, "email", "owner@test.com");
        ReflectionTestUtils.setField(owner, "point", AiPolicy.SUMMARY_POINT_COST * 2); // 잔액 충분한 상태

        other = new User();
        ReflectionTestUtils.setField(other, "id", 2L);
        ReflectionTestUtils.setField(other, "email", "other@test.com");
        ReflectionTestUtils.setField(other, "point", AiPolicy.SUMMARY_POINT_COST * 2);

        post = new Post();
        ReflectionTestUtils.setField(post, "id", 10L);
        ReflectionTestUtils.setField(post, "user", owner);
        ReflectionTestUtils.setField(post, "content", "TIL 본문 내용");
    }

    // ─── summarize() ─────────────────────────────────────────────────

    @Test
    @DisplayName("요약 성공 → 포인트 차감, PointLog 저장, 응답 반환")
    void summarize_success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(aiPromptClient.summarizeTil(any())).willReturn(MOCK_SUMMARY_JSON);

        // when
        AiSummaryResponse response = aiService.summarize(new AiSummaryRequest(10L), owner);

        // then
        assertThat(response.summary()).isEqualTo("핵심 요약 내용");
        assertThat(response.keyPoints()).containsExactly("포인트1", "포인트2", "포인트3");
        assertThat(response.usedPoint()).isEqualTo(AiPolicy.SUMMARY_POINT_COST);
        assertThat(response.remainPoint()).isEqualTo(AiPolicy.SUMMARY_POINT_COST * 2 - AiPolicy.SUMMARY_POINT_COST);
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("포인트 부족 시 402 — OpenAI 미호출")
    void summarize_insufficientPoint() {
        // given: 잔액이 비용보다 1 부족한 상태
        ReflectionTestUtils.setField(owner, "point", AiPolicy.SUMMARY_POINT_COST - 1);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(aiPromptClient, never()).summarizeTil(any());
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("TIL 미존재 시 404")
    void summarize_tilNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(999L), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("타인 TIL 요약 시도 → 403")
    void summarize_forbidden_when_not_owner() {
        // given: post의 소유자는 owner, 요청자는 other
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L), other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(aiPromptClient, never()).summarizeTil(any());
    }

    @Test
    @DisplayName("사용자 미존재 시 404")
    void summarize_userNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── generateQuiz() ──────────────────────────────────────────────

    @Test
    @DisplayName("퀴즈 생성 성공 → 포인트 count×10 차감, PointLog 저장, 응답 반환")
    void generateQuiz_success() {
        // given: count=2, 총 비용 = 2 × 10 = 20
        int count = 2;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost * 2);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(aiPromptClient.generateQuiz(any(), eq(count))).willReturn(MOCK_QUIZ_JSON);

        // when
        AiQuizResponse response = aiService.generateQuiz(new AiQuizRequest(10L, count), owner);

        // then
        assertThat(response.quizzes()).hasSize(count);
        assertThat(response.quizzes().get(0).question()).isEqualTo("질문1");
        assertThat(response.quizzes().get(0).answer()).isEqualTo("정답1");
        assertThat(response.quizzes().get(0).hint()).isEqualTo("힌트1");
        assertThat(response.usedPoint()).isEqualTo(totalCost);
        assertThat(response.remainPoint()).isEqualTo(totalCost);
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("퀴즈 포인트 부족 시 402 — OpenAI 미호출")
    void generateQuiz_insufficientPoint() {
        // given: count=5, 총 비용 50P, 잔액 49P
        int count = 5;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost - 1);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(10L, count), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(aiPromptClient, never()).generateQuiz(any(), anyInt());
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("타인 TIL로 퀴즈 생성 시도 → 403")
    void generateQuiz_forbidden_when_not_owner() {
        // given
        ReflectionTestUtils.setField(other, "point", 100);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(10L, 3), other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(aiPromptClient, never()).generateQuiz(any(), anyInt());
    }

    @Test
    @DisplayName("퀴즈 TIL 미존재 시 404")
    void generateQuiz_tilNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(999L, 3), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
