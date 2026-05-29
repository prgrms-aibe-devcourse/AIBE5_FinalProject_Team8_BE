package com.Rootin.domain.ai.controller;

import com.Rootin.domain.ai.constant.AiPolicy;
import com.Rootin.domain.ai.dto.AiQuizItem;
import com.Rootin.domain.ai.dto.AiQuizRequest;
import com.Rootin.domain.ai.dto.AiQuizResponse;
import com.Rootin.domain.ai.dto.AiSummaryRequest;
import com.Rootin.domain.ai.dto.AiSummaryResponse;
import com.Rootin.domain.ai.service.AiService;
import com.Rootin.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@ActiveProfiles("test")
@Import(com.Rootin.global.config.TestSecurityConfig.class)
class AiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AiService aiService;

    // ─── POST /ai/summary ────────────────────────────────────────────

    @Test
    @DisplayName("요약 성공 → 200 + 응답 본문 확인")
    @WithMockUser
    void summary_success() throws Exception {
        int used = AiPolicy.SUMMARY_POINT_COST;
        int remain = AiPolicy.SUMMARY_POINT_COST;
        AiSummaryResponse response = new AiSummaryResponse(
                "핵심 요약 내용",
                List.of("포인트1", "포인트2"),
                used,
                remain
        );
        given(aiService.summarize(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("핵심 요약 내용"))
                .andExpect(jsonPath("$.keyPoints[0]").value("포인트1"))
                .andExpect(jsonPath("$.usedPoint").value(AiPolicy.SUMMARY_POINT_COST))
                .andExpect(jsonPath("$.remainPoint").value(AiPolicy.SUMMARY_POINT_COST));
    }

    @Test
    @DisplayName("potId 누락 → 400")
    @WithMockUser
    void summary_badRequest_when_potId_missing() throws Exception {
        mockMvc.perform(post("/api/v1/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 부족 → 402")
    @WithMockUser
    void summary_paymentRequired_when_insufficient_point() throws Exception {
        given(aiService.summarize(any(), any()))
                .willThrow(new CustomException(HttpStatus.PAYMENT_REQUIRED, "포인트가 부족합니다."));

        mockMvc.perform(post("/api/v1/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("타인 화분 → 403")
    @WithMockUser
    void summary_forbidden_when_not_owner() throws Exception {
        given(aiService.summarize(any(), any()))
                .willThrow(new CustomException(HttpStatus.FORBIDDEN, "본인의 화분만 요약할 수 있습니다."));

        mockMvc.perform(post("/api/v1/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("summary는 TestSecurityConfig 적용 시 인증 없이도 200")
    void summary_success_without_auth_by_test_security_config() throws Exception {
        mockMvc.perform(post("/api/v1/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isOk());
    }

    // ─── POST /ai/quiz ───────────────────────────────────────────────

    @Test
    @DisplayName("퀴즈 생성 성공 → 200 + 응답 본문 확인")
    @WithMockUser
    void quiz_success() throws Exception {
        int count = 2;
        int totalCost = count * AiPolicy.QUIZ_POINT_COST_PER_QUESTION;
        AiQuizResponse response = new AiQuizResponse(
                List.of(
                        new AiQuizItem("질문1", List.of("정답1", "오답1", "오답2", "오답3"), "정답1", "힌트1"),
                        new AiQuizItem("질문2", List.of("오답1", "정답2", "오답2", "오답3"), "정답2", "힌트2")
                ),
                totalCost,
                totalCost
        );
        given(aiService.generateQuiz(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/ai/quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiQuizRequest(1L, count))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizzes[0].question").value("질문1"))
                .andExpect(jsonPath("$.quizzes[0].choices[0]").value("정답1"))
                .andExpect(jsonPath("$.quizzes[0].answer").value("정답1"))
                .andExpect(jsonPath("$.quizzes[0].hint").value("힌트1"))
                .andExpect(jsonPath("$.usedPoint").value(totalCost))
                .andExpect(jsonPath("$.remainPoint").value(totalCost));
    }

    @Test
    @DisplayName("count 누락 → 400")
    @WithMockUser
    void quiz_badRequest_when_count_missing() throws Exception {
        mockMvc.perform(post("/api/v1/ai/quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"potId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("count 최대값 초과 → 400")
    @WithMockUser
    void quiz_badRequest_when_count_exceeds_max() throws Exception {
        mockMvc.perform(post("/api/v1/ai/quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"potId\":1,\"count\":" + (AiPolicy.QUIZ_MAX_COUNT + 1) + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("퀴즈 포인트 부족 → 402")
    @WithMockUser
    void quiz_paymentRequired_when_insufficient_point() throws Exception {
        given(aiService.generateQuiz(any(), any()))
                .willThrow(new CustomException(HttpStatus.PAYMENT_REQUIRED, "포인트가 부족합니다."));

        mockMvc.perform(post("/api/v1/ai/quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiQuizRequest(1L, 3))))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("quiz는 TestSecurityConfig 적용 시 인증 없이도 200")
    void quiz_success_without_auth_by_test_security_config() throws Exception {
        mockMvc.perform(post("/api/v1/ai/quiz").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiQuizRequest(1L, 3))))
                .andExpect(status().isOk());
    }
}
