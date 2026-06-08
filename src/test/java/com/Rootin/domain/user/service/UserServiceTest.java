package com.Rootin.domain.user.service;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.dto.PasswordChangeRequest;
import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Mock
    private TilRepository tilRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

    // ─── getUserMe ────────────────────────────────────────────────────────

    @Test
    @DisplayName("내 정보 조회 성공 — provider, tilCount 포함 반환")
    void getUserMe_success() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .point(100)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tilRepository.countByUserIdAndStatus(1L, PostStatus.PUBLISHED)).willReturn(5L);

        // when
        UserMeResponse response = userService.getUserMe(1L);

        // then
        assertThat(response.getProvider()).isEqualTo("LOCAL");
        assertThat(response.getTilCount()).isEqualTo(5L);
        assertThat(response.getEmail()).isEqualTo("test@rootin.com");
        assertThat(response.getPoint()).isEqualTo(100);
    }

    @Test
    @DisplayName("내 정보 조회 — 존재하지 않는 userId → CustomException(404)")
    void getUserMe_userNotFound() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserMe(99L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("내 정보 조회 — TIL 없는 경우 tilCount = 0")
    void getUserMe_noTil_tilCountZero() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.GOOGLE)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tilRepository.countByUserIdAndStatus(1L, PostStatus.PUBLISHED)).willReturn(0L);

        // when
        UserMeResponse response = userService.getUserMe(1L);

        // then
        assertThat(response.getTilCount()).isZero();
        assertThat(response.getProvider()).isEqualTo("GOOGLE");
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
                .provider(Provider.LOCAL)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tilRepository.countByUserIdAndStatus(1L, PostStatus.PUBLISHED)).willReturn(3L);

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
        assertThat(response.getTilCount()).isEqualTo(3L);
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
                .provider(Provider.LOCAL)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tilRepository.countByUserIdAndStatus(1L, PostStatus.PUBLISHED)).willReturn(0L);

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

    // ─── changePassword ───────────────────────────────────────────────────

    @Test
    @DisplayName("비밀번호 변경 성공 — 현재 비밀번호 검증 후 새 비밀번호로 업데이트")
    void changePassword_success() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .password("encoded_current")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("currentPw123!", "encoded_current")).willReturn(true);
        given(passwordEncoder.encode("newPw123!")).willReturn("encoded_new");

        PasswordChangeRequest request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", "currentPw123!");
        ReflectionTestUtils.setField(request, "newPassword", "newPw123!");
        ReflectionTestUtils.setField(request, "confirmPassword", "newPw123!");

        // when
        userService.changePassword(1L, request);

        // then
        assertThat(user.getPassword()).isEqualTo("encoded_new");
    }

    @Test
    @DisplayName("비밀번호 변경 — 현재 비밀번호 불일치 → CustomException(400)")
    void changePassword_currentPasswordMismatch_throws() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .password("encoded_current")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw!", "encoded_current")).willReturn(false);

        PasswordChangeRequest request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", "wrongPw!");
        ReflectionTestUtils.setField(request, "newPassword", "newPw123!");
        ReflectionTestUtils.setField(request, "confirmPassword", "newPw123!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 — 새 비밀번호 불일치 → CustomException(400)")
    void changePassword_newPasswordConfirmMismatch_throws() {
        // given
        User user = User.builder()
                .email("test@rootin.com")
                .nickname("루틴이")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .password("encoded_current")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("currentPw123!", "encoded_current")).willReturn(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", "currentPw123!");
        ReflectionTestUtils.setField(request, "newPassword", "newPw123!");
        ReflectionTestUtils.setField(request, "confirmPassword", "differentPw123!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 — 소셜 로그인 유저(GOOGLE) → CustomException(403)")
    void changePassword_googleUser_throws() {
        // given
        User user = User.builder()
                .email("google@rootin.com")
                .nickname("구글유저")
                .role(Role.USER)
                .provider(Provider.GOOGLE)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        PasswordChangeRequest request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", "currentPw123!");
        ReflectionTestUtils.setField(request, "newPassword", "newPw123!");
        ReflectionTestUtils.setField(request, "confirmPassword", "newPw123!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
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
