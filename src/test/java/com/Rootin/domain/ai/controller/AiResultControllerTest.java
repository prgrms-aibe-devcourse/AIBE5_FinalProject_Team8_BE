package com.Rootin.domain.ai.controller;

import com.Rootin.domain.ai.service.AiResultService;
import com.Rootin.global.jwt.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiResultController.class)
@ActiveProfiles("test")
@Import(com.Rootin.global.config.TestSecurityConfig.class)
class AiResultControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AiResultService aiResultService;

    private JwtUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ─── POST /ai/results ────────────────────────────────────────────

    @Test @DisplayName("type 누락 → 400")
    void save_badRequest_when_type_missing() throws Exception {
        mockMvc.perform(post("/api/v1/ai/results").with(csrf()).with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"potId\":1,\"content\":\"요약\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test @DisplayName("potId 누락 → 400")
    void save_badRequest_when_potId_missing() throws Exception {
        mockMvc.perform(post("/api/v1/ai/results").with(csrf()).with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"content\":\"요약\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test @DisplayName("content 누락 → 400")
    void save_badRequest_when_content_missing() throws Exception {
        mockMvc.perform(post("/api/v1/ai/results").with(csrf()).with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"potId\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test @DisplayName("POST는 TestSecurityConfig 적용 시 인증 없이도 200")
    void save_success_without_auth_by_test_security_config() throws Exception {
        mockMvc.perform(post("/api/v1/ai/results").with(csrf()).with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUMMARY\",\"potId\":1,\"content\":\"요약\"}"))
                .andExpect(status().isOk());
    }

    // ─── GET /ai/results ─────────────────────────────────────────────

    @Test @DisplayName("GET /ai/results → 200")
    void getResults_success() throws Exception {
        given(aiResultService.getResults(any(), any())).willReturn(List.of());
        mockMvc.perform(get("/api/v1/ai/results").with(csrf()).with(user(mockUserDetails)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /ai/results?potId=1 → 200")
    void getResults_with_potId_success() throws Exception {
        given(aiResultService.getResults(any(), any())).willReturn(List.of());
        mockMvc.perform(get("/api/v1/ai/results").param("potId", "1")
                        .with(csrf()).with(user(mockUserDetails)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET은 TestSecurityConfig 적용 시 인증 없이도 200")
    void getResults_success_without_auth_by_test_security_config() throws Exception {
        given(aiResultService.getResults(any(), any())).willReturn(List.of());
        mockMvc.perform(get("/api/v1/ai/results").with(user(mockUserDetails)))
                .andExpect(status().isOk());
    }

    // ─── DELETE /ai/results/{resultId} ───────────────────────────────

    @Test @DisplayName("DELETE /ai/results/1 → 204 No Content")
    void delete_success() throws Exception {
        doNothing().when(aiResultService).delete(any(), any());
        mockMvc.perform(delete("/api/v1/ai/results/1").with(csrf()).with(user(mockUserDetails)))
                .andExpect(status().isNoContent());
    }

    @Test @DisplayName("DELETE는 TestSecurityConfig 적용 시 인증 없이도 204")
    void delete_success_without_auth_by_test_security_config() throws Exception {
        doNothing().when(aiResultService).delete(any(), any());
        mockMvc.perform(delete("/api/v1/ai/results/1").with(csrf()).with(user(mockUserDetails)))
                .andExpect(status().isNoContent());
    }
}
