package com.Rootin.domain.user.service;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // ─── deleteUser ───────────────────────────────────────────────────────

    @Test
    @DisplayName("회원 탈퇴 성공 — isDeleted=true, deletedAt 기록, RefreshToken 삭제")
    void deleteUser_success() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(1L);

        // then
        assertThat(user.isEnabled()).isFalse();          // isDeleted=true → isEnabled=false
        assertThat(user.getDeletedAt()).isNotNull();
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("회원 탈퇴 — 존재하지 않는 userId → CustomException(404)")
    void deleteUser_userNotFound() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(CustomException.class);
    }

    // ─── updateUserMe ─────────────────────────────────────────────────────

    @Test
    @DisplayName("프로필 수정 성공 — profileImageUrl 포함 시 profileImage 업데이트")
    void updateUserMe_withProfileImageUrl_success() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .profileImage("https://old.url/image.jpg")
                .role(Role.USER)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "루틴이");
        ReflectionTestUtils.setField(request, "bio", "새 소개");
        ReflectionTestUtils.setField(request, "profileImageUrl", "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/new.jpg");

        // when
        UserMeResponse response = userService.updateUserMe(1L, request);

        // then
        assertThat(response.getProfileImageUrl())
                .isEqualTo("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/new.jpg");
        assertThat(user.getProfileImage())
                .isEqualTo("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/new.jpg");
    }

    @Test
    @DisplayName("프로필 수정 성공 — profileImageUrl null 시 기존 이미지 유지")
    void updateUserMe_withoutProfileImageUrl_imageUnchanged() {
        // given
        String existingImageUrl = "https://old.url/image.jpg";
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .profileImage(existingImageUrl)
                .role(Role.USER)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "루틴이");
        ReflectionTestUtils.setField(request, "bio", null);
        ReflectionTestUtils.setField(request, "profileImageUrl", null);

        // when
        UserMeResponse response = userService.updateUserMe(1L, request);

        // then
        assertThat(response.getProfileImageUrl()).isEqualTo(existingImageUrl);
        assertThat(user.getProfileImage()).isEqualTo(existingImageUrl);
    }

    @Test
    @DisplayName("프로필 수정 — 중복 닉네임 → CustomException(400)")
    void updateUserMe_duplicateNickname_throws() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("다른닉네임")).willReturn(true);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "다른닉네임");

        // when & then
        assertThatThrownBy(() -> userService.updateUserMe(1L, request))
                .isInstanceOf(CustomException.class);
    }
}
