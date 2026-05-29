package com.Rootin.global.security;

import com.Rootin.global.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class AiSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("미인증 사용자는 AI 요약 API 호출 시 401")
    void summary_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(post("/api/v1/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"potId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 AI 퀴즈 API 호출 시 401")
    void quiz_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(post("/api/v1/ai/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"potId\":1,\"count\":3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 AI 결과 저장 API 호출 시 401")
    void saveResult_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(post("/api/v1/ai/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"potId\":1,\"content\":\"요약\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 AI 결과 목록 API 호출 시 401")
    void getResults_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(get("/api/v1/ai/results"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 AI 결과 삭제 API 호출 시 401")
    void deleteResult_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/results/1"))
                .andExpect(status().isUnauthorized());
    }
}
