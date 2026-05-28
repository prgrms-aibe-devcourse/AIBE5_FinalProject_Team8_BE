package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PlantInfoResponse;
import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.service.GardenDashboardService;
import com.Rootin.domain.garden.service.PotService;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
@Import(PotControllerTest.TestSecurityConfig.class)
class PotControllerTest {

    // 테스트 환경에서는 스프링 시큐리티의 모든 인증 필터를 통과(permitAll)시키는 설정을 주입합니다.
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PotService potService;

    @MockBean
    private GardenDashboardService gardenDashboardService;

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
        mockMvc.perform(post("/api/v1/pots")
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
        mockMvc.perform(post("/api/v1/pots")
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
        PotSummaryResponse response = new PotSummaryResponse(
                10L,
                "자바 화분",
                "자바 기초 학습용 화분",
                1,
                0,
                true,
                "기본 씨앗",
                GrowthStage.SEED
        );

        given(potService.getPots(userId)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/pots")
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("자바 화분"))
                .andExpect(jsonPath("$[0].plantName").value("기본 씨앗"))
                .andExpect(jsonPath("$[0].growthStage").value("SEED"));
    }

    @Test
    @DisplayName("화분 대시보드 API 호출 시 200 OK와 상세 정보 조립 결과를 반환한다")
    void getGardenDashboardSuccess() throws Exception {
        // given
        Long userId = 1L;
        Long potId = 10L;

        PlantInfoResponse plantInfo = new PlantInfoResponse(
                "기본 씨앗",
                GrowthStage.SEED,
                "http://image.url",
                "http://silhouette.url"
        );

        GardenInfoResponse response = new GardenInfoResponse(
                potId,
                "내 공부 화분",
                "소개글",
                2,
                150,
                50,
                200,
                25.0,
                5L,
                3,
                java.time.LocalDateTime.of(2026, 5, 27, 12, 0),
                plantInfo
        );

        given(gardenDashboardService.getGardenDashboard(potId, userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/pots/{potId}/dashboard", potId)
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.potId").value(potId))
                .andExpect(jsonPath("$.title").value("내 공부 화분"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.totalExp").value(150))
                .andExpect(jsonPath("$.currentLevelExp").value(50))
                .andExpect(jsonPath("$.nextLevelExpRequired").value(200))
                .andExpect(jsonPath("$.progressPercentage").value(25.0))
                .andExpect(jsonPath("$.totalTilCount").value(5))
                .andExpect(jsonPath("$.streakDays").value(3))
                .andExpect(jsonPath("$.lastWateredAt").value("2026-05-27T12:00:00"))
                .andExpect(jsonPath("$.plant.name").value("기본 씨앗"))
                .andExpect(jsonPath("$.plant.growthStage").value("SEED"));
    }

    @Test
    @DisplayName("다른 사용자의 화분 대시보드를 조회하면 403 Forbidden 에러를 반환한다")
    void getGardenDashboardForbidden() throws Exception {
        // given
        Long userId = 2L; // 화분 소유주가 아닌 다른 사용자 ID
        Long potId = 10L;

        given(gardenDashboardService.getGardenDashboard(potId, userId))
                .willThrow(com.Rootin.global.exception.CustomException.forbidden("해당 화분의 대시보드에 접근할 권한이 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/pots/{potId}/dashboard", potId)
                        .header("X-USER-ID", userId))
                .andExpect(status().isForbidden());
    }
}
