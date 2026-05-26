package com.Rootin.domain.ai.controller;

import com.Rootin.domain.ai.service.AiResultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiResultController.class)
@ActiveProfiles("test")
class AiResultControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AiResultService aiResultService;

    @Test
    @DisplayName("type 누락 → 400 Bad Request")
    @WithMockUser
    void save_badRequest_when_type_missing() throws Exception {
        mockMvc.perform(post("/ai/results")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tilId\":1,\"content\":\"요약 내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("tilId 누락 → 400 Bad Request")
    @WithMockUser
    void save_badRequest_when_tilId_missing() throws Exception {
        mockMvc.perform(post("/ai/results")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"content\":\"요약 내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("content 누락 → 400 Bad Request")
    @WithMockUser
    void save_badRequest_when_content_missing() throws Exception {
        mockMvc.perform(post("/ai/results")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"tilId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("인증 없이 요청 → 401 Unauthorized")
    void save_unauthorized_when_no_auth() throws Exception {
        mockMvc.perform(post("/ai/results")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"tilId\":1,\"content\":\"요약 내용\"}"))
                .andExpect(status().isUnauthorized());
    }
}
