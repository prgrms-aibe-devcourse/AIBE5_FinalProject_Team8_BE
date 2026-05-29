package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.jwt.JwtUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import(com.Rootin.global.config.TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    // ─── GET /api/v1/users/me ─────────────────────────────────────────────

    @Test
    @DisplayName("유저 정보 조회 성공 → 200 + 응답 필드 확인")
    void getMe_success() throws Exception {
        // given
        User mockUser = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .profileImage("https://cdn.rootin.com/profile/1.jpg")
                .point(100)
                .role(Role.USER)
                .build();

        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserMeResponse response = UserMeResponse.of(mockUser);
        given(userService.getUserMe(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .with(user(jwtUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@rootin.com"))
                .andExpect(jsonPath("$.data.nickname").value("루틴이"))
                .andExpect(jsonPath("$.data.profileImage").value("https://cdn.rootin.com/profile/1.jpg"))
                .andExpect(jsonPath("$.data.point").value(100));
    }

    @Test
    @DisplayName("비인증 요청 → 400")
    void getMe_unauthenticated() throws Exception {
        // @AuthenticationPrincipal은 null → CustomException.badRequest → 400
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isBadRequest());
    }
}
