package com.Rootin.domain.ai.controller;

import com.Rootin.domain.ai.dto.AiSummaryRequest;
import com.Rootin.domain.ai.dto.AiSummaryResponse;
import com.Rootin.domain.ai.constant.AiPolicy;
import com.Rootin.domain.ai.service.AiService;
import com.Rootin.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

        mockMvc.perform(post("/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("핵심 요약 내용"))
                .andExpect(jsonPath("$.keyPoints[0]").value("포인트1"))
                .andExpect(jsonPath("$.usedPoint").value(AiPolicy.SUMMARY_POINT_COST))
                .andExpect(jsonPath("$.remainPoint").value(AiPolicy.SUMMARY_POINT_COST));
    }

    @Test
    @DisplayName("tilId 누락 → 400")
    @WithMockUser
    void summary_badRequest_when_tilId_missing() throws Exception {
        mockMvc.perform(post("/ai/summary").with(csrf())
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

        mockMvc.perform(post("/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("타인 TIL → 403")
    @WithMockUser
    void summary_forbidden_when_not_owner() throws Exception {
        given(aiService.summarize(any(), any()))
                .willThrow(new CustomException(HttpStatus.FORBIDDEN, "본인의 TIL만 요약할 수 있습니다."));

        mockMvc.perform(post("/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 → 401")
    void summary_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(post("/ai/summary").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiSummaryRequest(1L))))
                .andExpect(status().isUnauthorized());
    }
}
