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
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
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

import java.util.List;
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
    private PotRepository potRepository;

    @Mock
    private TilRepository tilRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private User owner;
    private User other;
    private Pot pot;
    private Til til;

    private static final String MOCK_SUMMARY_JSON =
            "{\"summary\":\"핵심 요약 내용\",\"keyPoints\":[\"포인트1\",\"포인트2\",\"포인트3\"]}";

    private static final String MOCK_QUIZ_JSON =
            "{\"quizzes\":[" +
            "{\"question\":\"질문1\",\"choices\":[\"정답1\",\"오답1\",\"오답2\",\"오답3\"],\"answer\":\"정답1\",\"hint\":\"힌트1\"}," +
            "{\"question\":\"질문2\",\"choices\":[\"오답1\",\"정답2\",\"오답2\",\"오답3\"],\"answer\":\"정답2\",\"hint\":\"힌트2\"}" +
            "]}";

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);
        ReflectionTestUtils.setField(owner, "email", "owner@test.com");
        ReflectionTestUtils.setField(owner, "point", AiPolicy.SUMMARY_POINT_COST * 2);

        other = new User();
        ReflectionTestUtils.setField(other, "id", 2L);
        ReflectionTestUtils.setField(other, "email", "other@test.com");
        ReflectionTestUtils.setField(other, "point", AiPolicy.SUMMARY_POINT_COST * 2);

        pot = Pot.builder()
                .userId(1L)
                .title("테스트 화분")
                .level(1)
                .totalExp(0)
                .build();
        ReflectionTestUtils.setField(pot, "id", 10L);

        til = new Til();
        ReflectionTestUtils.setField(til, "id", 100L);
        ReflectionTestUtils.setField(til, "user", owner);
        ReflectionTestUtils.setField(til, "content", "TIL 본문 내용");
    }

    // --- summarize() -------------------------------------------------

    @Test
    @DisplayName("요약 성공 -> 포인트 차감, PointLog 저장, 응답 반환")
    void summarize_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of(til));
        given(aiPromptClient.summarizeTil(any())).willReturn(MOCK_SUMMARY_JSON);

        AiSummaryResponse response = aiService.summarize(new AiSummaryRequest(10L, null), 1L);

        assertThat(response.summary()).isEqualTo("핵심 요약 내용");
        assertThat(response.keyPoints()).containsExactly("포인트1", "포인트2", "포인트3");
        assertThat(response.usedPoint()).isEqualTo(AiPolicy.SUMMARY_POINT_COST);
        assertThat(response.remainPoint()).isEqualTo(AiPolicy.SUMMARY_POINT_COST * 2 - AiPolicy.SUMMARY_POINT_COST);
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("포인트 부족 시 402 -- OpenAI 미호출")
    void summarize_insufficientPoint() {
        ReflectionTestUtils.setField(owner, "point", AiPolicy.SUMMARY_POINT_COST - 1);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(aiPromptClient, never()).summarizeTil(any());
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("화분 미존재 시 404")
    void summarize_potNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(999L, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("타인 화분 요약 시도 -> 403")
    void summarize_forbidden_when_not_owner() {
        Pot ownerPot = Pot.builder().userId(1L).title("오너 화분").level(1).totalExp(0).build();
        ReflectionTestUtils.setField(ownerPot, "id", 10L);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(potRepository.findById(10L)).willReturn(Optional.of(ownerPot));

        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L, null), 2L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(aiPromptClient, never()).summarizeTil(any());
    }

    @Test
    @DisplayName("화분에 TIL 없음 -> 404")
    void summarize_emptyTils() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of());

        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("사용자 미존재 시 404")
    void summarize_userNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.summarize(new AiSummaryRequest(10L, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- generateQuiz() ----------------------------------------------

    @Test
    @DisplayName("퀴즈 생성 성공 -> 포인트 count×10 차감, PointLog 저장, 응답 반환")
    void generateQuiz_success() {
        int count = 2;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost * 2);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of(til));
        given(aiPromptClient.generateQuiz(any(), eq(count))).willReturn(MOCK_QUIZ_JSON);

        AiQuizResponse response = aiService.generateQuiz(new AiQuizRequest(10L, count, null), 1L);

        assertThat(response.quizzes()).hasSize(count);
        assertThat(response.quizzes().get(0).question()).isEqualTo("질문1");
        assertThat(response.quizzes().get(0).answer()).isEqualTo("정답1");
        assertThat(response.quizzes().get(0).hint()).isEqualTo("힌트1");
        assertThat(response.usedPoint()).isEqualTo(totalCost);
        assertThat(response.remainPoint()).isEqualTo(totalCost);
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("퀴즈 포인트 부족 시 402 -- OpenAI 미호출")
    void generateQuiz_insufficientPoint() {
        int count = 5;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost - 1);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(10L, count, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(aiPromptClient, never()).generateQuiz(any(), anyInt());
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("타인 화분으로 퀴즈 생성 시도 -> 403")
    void generateQuiz_forbidden_when_not_owner() {
        ReflectionTestUtils.setField(other, "point", 100);
        Pot ownerPot = Pot.builder().userId(1L).title("오너 화분").level(1).totalExp(0).build();
        ReflectionTestUtils.setField(ownerPot, "id", 10L);

        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(potRepository.findById(10L)).willReturn(Optional.of(ownerPot));

        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(10L, 3, null), 2L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(aiPromptClient, never()).generateQuiz(any(), anyInt());
    }

    @Test
    @DisplayName("퀴즈 화분 미존재 시 404")
    void generateQuiz_potNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(999L, 3, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("화분에 TIL 없음 -> 404")
    void generateQuiz_emptyTils() {
        int count = 3;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost * 2);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of());

        assertThatThrownBy(() -> aiService.generateQuiz(new AiQuizRequest(10L, count, null), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- summarize() -- tilIds 선택 경로 -----------------------------

    @Test
    @DisplayName("tilIds 지정 요약 성공 -> 선택된 TIL만 사용, 포인트 차감")
    void summarize_withTilIds_success() {
        Til til2 = new Til();
        ReflectionTestUtils.setField(til2, "id", 101L);
        ReflectionTestUtils.setField(til2, "user", owner);
        ReflectionTestUtils.setField(til2, "content", "두 번째 TIL 본문");

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(tilRepository.findAllByIdInAndStatusAndUserId(List.of(100L, 101L), PostStatus.PUBLISHED, 1L)).willReturn(List.of(til, til2));
        given(aiPromptClient.summarizeTil(any())).willReturn(MOCK_SUMMARY_JSON);

        AiSummaryResponse response = aiService.summarize(
                new AiSummaryRequest(10L, List.of(100L, 101L)), 1L);

        assertThat(response.summary()).isEqualTo("핵심 요약 내용");
        verify(potRepository, never()).findById(any());
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("tilIds에 타인 TIL 포함 시 400 -- DB 레벨 필터로 size 불일치 감지, OpenAI 미호출")
    void summarize_withTilIds_containsOtherUserTil_400() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        // userId 조건으로 DB에서 타인 TIL 제외 → size 불일치 → 400
        given(tilRepository.findAllByIdInAndStatusAndUserId(List.of(100L, 200L), PostStatus.PUBLISHED, 1L)).willReturn(List.of(til));

        assertThatThrownBy(() -> aiService.summarize(
                new AiSummaryRequest(10L, List.of(100L, 200L)), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(aiPromptClient, never()).summarizeTil(any());
        verify(pointLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("tilIds 전부 존재하지 않으면 404 -- OpenAI 미호출")
    void summarize_withTilIds_notFound_404() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(tilRepository.findAllByIdInAndStatusAndUserId(List.of(999L), PostStatus.PUBLISHED, 1L)).willReturn(List.of());

        assertThatThrownBy(() -> aiService.summarize(
                new AiSummaryRequest(10L, List.of(999L)), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(aiPromptClient, never()).summarizeTil(any());
    }

    // --- generateQuiz() -- tilIds 선택 경로 --------------------------

    @Test
    @DisplayName("tilIds 지정 퀴즈 생성 성공 -> 선택된 TIL만 사용, 포인트 차감")
    void generateQuiz_withTilIds_success() {
        int count = 2;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost * 2);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(tilRepository.findAllByIdInAndStatusAndUserId(List.of(100L), PostStatus.PUBLISHED, 1L)).willReturn(List.of(til));
        given(aiPromptClient.generateQuiz(any(), eq(count))).willReturn(MOCK_QUIZ_JSON);

        AiQuizResponse response = aiService.generateQuiz(
                new AiQuizRequest(10L, count, List.of(100L)), 1L);

        assertThat(response.quizzes()).hasSize(count);
        verify(potRepository, never()).findById(any());
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("tilIds에 타인 TIL 포함 시 퀴즈 400 -- DB 레벨 필터로 size 불일치 감지, OpenAI 미호출")
    void generateQuiz_withTilIds_containsOtherUserTil_400() {
        int count = 2;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        ReflectionTestUtils.setField(owner, "point", totalCost * 2);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        // userId 조건으로 DB에서 타인 TIL 제외 → size 불일치 → 400
        given(tilRepository.findAllByIdInAndStatusAndUserId(List.of(100L, 200L), PostStatus.PUBLISHED, 1L)).willReturn(List.of(til));

        assertThatThrownBy(() -> aiService.generateQuiz(
                new AiQuizRequest(10L, count, List.of(100L, 200L)), 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(aiPromptClient, never()).generateQuiz(any(), anyInt());
        verify(pointLogRepository, never()).save(any());
    }
}
