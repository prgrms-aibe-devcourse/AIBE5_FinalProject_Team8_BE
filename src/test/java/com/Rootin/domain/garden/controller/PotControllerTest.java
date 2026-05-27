package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.service.PotService;
import com.Rootin.global.annotation.ControllerTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(PotController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.Rootin.global.config.SecurityConfig.class)
class PotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PotService potService;

    @Test
    @DisplayName("화분 생성 API 호출 시 201 Created 코드를 반환하고 JSON 응답을 리턴한다")
    void createPotSuccess() throws Exception {
        // given
        Long userId = 1L;
        PotCreateRequest request = PotCreateRequest.builder()
                .title("자바 공부방")
                .description("자바 기초 학습용 화분")
                .build();
        PotResponse response = PotResponse.builder()
                .id(10L)
                .title("자바 공부방")
                .description("자바 기초 학습용 화분")
                .level(1)
                .totalExp(0)
                .isDisplayed(false)
                .build();

        given(potService.createPot(eq(userId), any(PotCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/pots")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("자바 공부방"))
                .andExpect(jsonPath("$.description").value("자바 기초 학습용 화분"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.totalExp").value(0));
    }

    @Test
    @DisplayName("화분 제목이 비어있으면 400 Bad Request 에러를 반환한다")
    void createPotValidationFail() throws Exception {
        // given
        Long userId = 1L;
        PotCreateRequest request = PotCreateRequest.builder()
                .title("") // 빈 제목으로 벨리데이션 오류 유발
                .build();

        // when & then
        mockMvc.perform(post("/api/pots")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인한 유저의 화분 목록을 조회하면 200 OK와 리스트를 반환한다")
    void getPotsSuccess() throws Exception {
        // given
        Long userId = 1L;
        PotResponse response = PotResponse.builder()
                .id(10L)
                .title("자바 화분")
                .build();

        given(potService.getPots(userId)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/pots")
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("자바 화분"));
    }
}
