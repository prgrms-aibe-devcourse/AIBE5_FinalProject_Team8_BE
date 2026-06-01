package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.jwt.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import(com.Rootin.global.config.TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/v1/users/me ───────────────────────────────────────────

    @Test
    @DisplayName("프로필 수정 성공 → 200 + 변경된 nickname/bio 확인")
    void updateMe_success() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserMeResponse response = UserMeResponse.builder()
                .id(1L)
                .email("test@rootin.com")
                .nickname("변경닉네임")
                .bio("안녕하세요")
                .point(100)
                .build();

        given(userService.updateUserMe(eq(1L), any(UserUpdateRequest.class))).willReturn(response);

        String body = objectMapper.writeValueAsString(
                new TestUserUpdateRequest("변경닉네임", "안녕하세요"));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("변경닉네임"))
                .andExpect(jsonPath("$.data.bio").value("안녕하세요"));
    }

    @Test
    @DisplayName("nickname 1자 → 400 유효성 검증 실패")
    void updateMe_nicknameTooShort() throws Exception {
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String body = objectMapper.writeValueAsString(
                new TestUserUpdateRequest("a", null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nickname 51자 → 400 유효성 검증 실패")
    void updateMe_nicknameTooLong() throws Exception {
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String tooLong = "a".repeat(51);
        String body = objectMapper.writeValueAsString(
                new TestUserUpdateRequest(tooLong, null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("bio 256자 → 400 유효성 검증 실패")
    void updateMe_bioTooLong() throws Exception {
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String tooLong = "a".repeat(256);
        String body = objectMapper.writeValueAsString(
                new TestUserUpdateRequest("닉네임", tooLong));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비인증 요청 → 400")
    void updateMe_unauthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(
                new TestUserUpdateRequest("닉네임", null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── 테스트용 내부 DTO (UserUpdateRequest는 @NoArgsConstructor만 있어 직렬화용) ───
    record TestUserUpdateRequest(String nickname, String bio) {}
}
