package com.Rootin.domain.garden.controller;

import com.Rootin.domain.garden.dto.GardenLayoutUpdateRequest;
import com.Rootin.domain.garden.dto.GardenResponse;
import com.Rootin.domain.garden.dto.HarvestedPlantResponse;
import com.Rootin.domain.garden.dto.LayoutUpdateDto;
import com.Rootin.domain.garden.dto.PotGardenResponse;
import com.Rootin.domain.garden.dto.ThemeUpdateRequest;
import com.Rootin.domain.garden.service.GardenService;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import com.Rootin.global.config.TestSecurityConfig;
import com.Rootin.global.jwt.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GardenController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class GardenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GardenService gardenService;

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
    @DisplayName("정원 조회 API는 JWT 인증 사용자 ID로 정원 정보를 조회한다")
    void getGardenSuccess() throws Exception {
        GardenResponse response = new GardenResponse(
                GardenTheme.FOREST,
                List.of(new PotGardenResponse(
                        10L,
                        "자바 화분",
                        3,
                        "씨앗몬",
                        GrowthStage.SPROUT,
                        "https://image.example/pot.png",
                        true,
                        100,
                        120
                )),
                List.of(new HarvestedPlantResponse(
                        20L,
                        1L,
                        "장미",
                        "https://image.example/rose.png",
                        false,
                        null,
                        null
                ))
        );

        given(gardenService.getGarden(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/garden")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("FOREST"))
                .andExpect(jsonPath("$.pots[0].id").value(10L))
                .andExpect(jsonPath("$.pots[0].plantName").value("씨앗몬"))
                .andExpect(jsonPath("$.pots[0].growthStage").value("SPROUT"))
                .andExpect(jsonPath("$.harvestedPlants[0].id").value(20L))
                .andExpect(jsonPath("$.harvestedPlants[0].name").value("장미"));

        verify(gardenService).getGarden(1L);
    }

    @Test
    @DisplayName("정원 테마 변경 API는 JWT 인증 사용자 ID로 테마를 변경한다")
    void updateThemeSuccess() throws Exception {
        ThemeUpdateRequest request = new ThemeUpdateRequest(GardenTheme.NIGHT);

        mockMvc.perform(patch("/api/v1/garden/theme")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gardenService).updateGardenTheme(1L, GardenTheme.NIGHT);
    }

    @Test
    @DisplayName("정원 배치 저장 API는 JWT 인증 사용자 ID로 배치 정보를 저장한다")
    void updateLayoutSuccess() throws Exception {
        GardenLayoutUpdateRequest request = new GardenLayoutUpdateRequest(
                List.of(new LayoutUpdateDto(10L, true, 100, 120)),
                List.of(new LayoutUpdateDto(20L, false, null, null))
        );

        mockMvc.perform(put("/api/v1/garden/layout")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gardenService).updateGardenLayout(eq(1L), any(GardenLayoutUpdateRequest.class));
    }
}
