package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PlantOptionResponse;
import com.Rootin.domain.garden.dto.PlantInfoResponse;
import com.Rootin.domain.garden.dto.PotPlantOptionsResponse;
import com.Rootin.domain.garden.dto.PotPlantRequest;
import com.Rootin.domain.garden.dto.PotPlantResponse;
import com.Rootin.domain.garden.dto.PotCreateRequest;
import com.Rootin.domain.garden.dto.PotResponse;
import com.Rootin.domain.garden.dto.PotSummaryResponse;
import com.Rootin.domain.garden.dto.PotUpdateRequest;
import com.Rootin.domain.garden.dto.PlantingType;
import com.Rootin.domain.garden.service.GardenDashboardService;
import com.Rootin.domain.garden.service.PotPlantService;
import com.Rootin.domain.garden.service.PotService;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.global.jwt.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(PotController.class)
@ActiveProfiles("test")
@Import(com.Rootin.global.config.TestSecurityConfig.class)
class PotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PotService potService;

    @MockBean
    private GardenDashboardService gardenDashboardService;

    @MockBean
    private PotPlantService potPlantService;

    private JwtUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new JwtUserDetails(
                1L,
                "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("화분 생성 API 호출 시 201 Created 코드를 반환하고 JSON 응답을 리턴한다")
    void createPotSuccess() throws Exception {
        // given
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

        given(potService.createPot(eq(1L), any(PotCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/pots")
                        .with(user(userDetails))
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
        PotCreateRequest request = PotCreateRequest.builder()
                .title("") // 빈 제목으로 벨리데이션 오류 유발
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/pots")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "12345678901, 소개글",
            "자바화분, 12345678901234567890123456"
    })
    @DisplayName("화분 생성 API 호출 시 제목 또는 소개글 길이 검증에 실패하면 400 Bad Request 에러를 반환한다")
    void createPotLengthValidationFail(String title, String description) throws Exception {
        // given
        PotCreateRequest request = PotCreateRequest.builder()
                .title(title)
                .description(description)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/pots")
                        .with(user(userDetails))
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
                GrowthStage.SEED,
                0
        );

        given(potService.getPots(userId)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/pots")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("자바 화분"))
                .andExpect(jsonPath("$[0].plantName").value("기본 씨앗"))
                .andExpect(jsonPath("$[0].growthStage").value("SEED"));
    }

    @Test
    @DisplayName("화분 수정 API 호출 시 200 OK와 수정된 화분 정보를 반환한다")
    void updatePotSuccess() throws Exception {
        // given
        Long userId = 1L;
        Long potId = 10L;
        String requestJson = """
                {
                  "title": "수정된 화분",
                  "description": "수정된 소개글"
                }
                """;
        PotResponse response = PotResponse.builder()
                .id(potId)
                .title("수정된 화분")
                .description("수정된 소개글")
                .level(2)
                .totalExp(150)
                .isDisplayed(false)
                .build();

        given(potService.updatePot(eq(potId), eq(userId), any(PotUpdateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/pots/{potId}", potId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(potId))
                .andExpect(jsonPath("$.title").value("수정된 화분"))
                .andExpect(jsonPath("$.description").value("수정된 소개글"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.totalExp").value(150));
    }

    @Test
    @DisplayName("화분 수정 API 호출 시 제목이 비어있으면 400 Bad Request 에러를 반환한다")
    void updatePotValidationFail() throws Exception {
        // given
        Long potId = 10L;
        String requestJson = """
                {
                  "title": "",
                  "description": "수정된 소개글"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/v1/pots/{potId}", potId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "12345678901, 수정된 소개글",
            "수정화분, 12345678901234567890123456"
    })
    @DisplayName("화분 수정 API 호출 시 제목 또는 소개글 길이 검증에 실패하면 400 Bad Request 에러를 반환한다")
    void updatePotLengthValidationFail(String title, String description) throws Exception {
        // given
        Long potId = 10L;
        String requestJson = objectMapper.writeValueAsString(
                java.util.Map.of("title", title, "description", description)
        );

        // when & then
        mockMvc.perform(patch("/api/v1/pots/{potId}", potId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
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
                "http://silhouette.url",
                0.0,
                false
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
                        .with(user(userDetails)))
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
        JwtUserDetails otherUserDetails = new JwtUserDetails(
                userId,
                "other@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(gardenDashboardService.getGardenDashboard(potId, userId))
                .willThrow(com.Rootin.global.exception.CustomException.forbidden("해당 화분의 대시보드에 접근할 권한이 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/pots/{potId}/dashboard", potId)
                        .with(user(otherUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("화분에 심을 식물 선택지 조회 API 호출 시 현재 식물과 수확 식물 목록을 반환한다")
    void getPlantOptionsSuccess() throws Exception {
        // given
        Long userId = 1L;
        Long potId = 10L;

        PotPlantResponse currentPlant = new PotPlantResponse(
                potId,
                100L,
                1L,
                "기본 씨앗",
                "common",
                GrowthStage.SEED,
                0
        );
        PlantOptionResponse harvestedPlant = new PlantOptionResponse(
                200L,
                2L,
                "버섯씨앗",
                "common",
                "http://mushroom.image",
                8,
                java.time.LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        PotPlantOptionsResponse response = new PotPlantOptionsResponse(
                potId,
                true,
                null,
                true,
                currentPlant,
                List.of(harvestedPlant)
        );

        given(potPlantService.getPlantOptions(userId, potId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/pots/{potId}/plant-options", potId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.potId").value(potId))
                .andExpect(jsonPath("$.canPlant").value(true))
                .andExpect(jsonPath("$.randomSeedAvailable").value(true))
                .andExpect(jsonPath("$.currentPlant.plantName").value("기본 씨앗"))
                .andExpect(jsonPath("$.harvestedPlants[0].sourcePlantItemId").value(200L))
                .andExpect(jsonPath("$.harvestedPlants[0].plantName").value("버섯씨앗"));
    }

    @Test
    @DisplayName("화분에 새 식물 심기 API 호출 시 200 OK와 심어진 식물 정보를 반환한다")
    void plantSuccess() throws Exception {
        // given
        Long userId = 1L;
        Long potId = 10L;
        PotPlantRequest request = new PotPlantRequest(PlantingType.RANDOM_SEED, null);
        PotPlantResponse response = new PotPlantResponse(
                potId,
                101L,
                3L,
                "달빛씨앗",
                "rare",
                GrowthStage.SEED,
                0
        );

        given(potPlantService.plant(eq(userId), eq(potId), any(PotPlantRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/pots/{potId}/plant", potId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.potId").value(potId))
                .andExpect(jsonPath("$.plantItemId").value(101L))
                .andExpect(jsonPath("$.plantName").value("달빛씨앗"))
                .andExpect(jsonPath("$.rarity").value("rare"))
                .andExpect(jsonPath("$.growthStage").value("SEED"))
                .andExpect(jsonPath("$.growthExp").value(0));
    }
}
