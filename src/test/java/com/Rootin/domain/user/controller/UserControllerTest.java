package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.jwt.JwtUserDetails;
import com.Rootin.global.s3.S3Service;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    S3Service s3Service;

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
                .provider(Provider.LOCAL)
                .build();

        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UserMeResponse response = UserMeResponse.of(mockUser, 7L);
        given(userService.getUserMe(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .with(user(jwtUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@rootin.com"))
                .andExpect(jsonPath("$.data.nickname").value("루틴이"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.rootin.com/profile/1.jpg"))
                .andExpect(jsonPath("$.data.point").value(100))
                .andExpect(jsonPath("$.data.provider").value("LOCAL"))
                .andExpect(jsonPath("$.data.tilCount").value(7));
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
    @DisplayName("nickname 21자 → 400 유효성 검증 실패 (경계값 초과)")
    void updateMe_nicknameTooLong() throws Exception {
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String tooLong = "a".repeat(21);
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

    // ─── DELETE /api/v1/users/me ──────────────────────────────────────────

    @Test
    @DisplayName("회원 탈퇴 성공 → 200")
    void deleteMe_success() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        willDoNothing().given(userService).deleteUser(1L);

        // when & then
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user(jwtUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("비인증 요청 → 400")
    void deleteMe_unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/users/me/profile-image/presigned-url ───────────────

    @Test
    @DisplayName("Presigned URL 발급 성공 — jpg 파일 → 200 + presignedUrl/fileUrl 반환")
    void getProfileImagePresignedUrl_success() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String fakePresignedUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg?X-Amz-Signature=abc";
        String fakeFileUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg";

        given(s3Service.generatePresignedPutUrl(anyString(), eq("image/jpeg"))).willReturn(fakePresignedUrl);
        given(s3Service.getFileUrl(anyString())).willReturn(fakeFileUrl);

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .with(user(jwtUserDetails))
                        .param("fileName", "profile.jpg")
                        .param("fileSize", "1048576"))   // 1MB
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").value(fakePresignedUrl))
                .andExpect(jsonPath("$.data.fileUrl").value(fakeFileUrl));
    }

    @Test
    @DisplayName("Presigned URL 발급 — 1MB 초과 파일 → 400")
    void getProfileImagePresignedUrl_fileTooLarge() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        long overLimit = 1024 * 1024L + 1; // 1MB + 1 byte

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .with(user(jwtUserDetails))
                        .param("fileName", "profile.jpg")
                        .param("fileSize", String.valueOf(overLimit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Presigned URL 발급 — 정확히 1MB → 200 (경계값)")
    void getProfileImagePresignedUrl_exactLimit_success() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String fakePresignedUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg?X-Amz-Signature=abc";
        String fakeFileUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg";

        given(s3Service.generatePresignedPutUrl(anyString(), eq("image/jpeg"))).willReturn(fakePresignedUrl);
        given(s3Service.getFileUrl(anyString())).willReturn(fakeFileUrl);

        long exactLimit = 1024 * 1024L; // 1MB

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .with(user(jwtUserDetails))
                        .param("fileName", "profile.jpg")
                        .param("fileSize", String.valueOf(exactLimit)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Presigned URL 발급 — 지원하지 않는 확장자(bmp) → 400")
    void getProfileImagePresignedUrl_unsupportedExtension() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .with(user(jwtUserDetails))
                        .param("fileName", "profile.bmp")
                        .param("fileSize", "1048576"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Presigned URL 발급 — 확장자 없는 파일명 → 400")
    void getProfileImagePresignedUrl_noExtension() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .with(user(jwtUserDetails))
                        .param("fileName", "profileimage")
                        .param("fileSize", "1048576"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Presigned URL 발급 — 비인증 요청 → 400")
    void getProfileImagePresignedUrl_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/profile-image/presigned-url")
                        .param("fileName", "profile.jpg")
                        .param("fileSize", "1048576"))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/v1/users/me/password ─────────────────────────────────

    @Test
    @DisplayName("비밀번호 변경 성공 → 200")
    void changePassword_success() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        willDoNothing().given(userService).changePassword(eq(1L), any());

        String body = objectMapper.writeValueAsString(
                new TestPasswordChangeRequest("currentPw123!", "newPw123!", "newPw123!"));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("비밀번호 변경 — confirmPassword 불일치 → 400 (@AssertTrue 검증)")
    void changePassword_confirmMismatch() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String body = objectMapper.writeValueAsString(
                new TestPasswordChangeRequest("currentPw123!", "newPw123!", "differentPw123!"));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 변경 — newPassword 8자 미만 → 400")
    void changePassword_newPasswordTooShort() throws Exception {
        // given
        JwtUserDetails jwtUserDetails = new JwtUserDetails(
                1L, "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String body = objectMapper.writeValueAsString(
                new TestPasswordChangeRequest("currentPw123!", "short", "short"));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(jwtUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 변경 — 비인증 요청 → 400")
    void changePassword_unauthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(
                new TestPasswordChangeRequest("currentPw123!", "newPw123!", "newPw123!"));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── 테스트용 내부 DTO (UserUpdateRequest는 @NoArgsConstructor만 있어 직렬화용) ───
    record TestUserUpdateRequest(String nickname, String bio) {}
    record TestPasswordChangeRequest(String currentPassword, String newPassword, String confirmPassword) {}
}
