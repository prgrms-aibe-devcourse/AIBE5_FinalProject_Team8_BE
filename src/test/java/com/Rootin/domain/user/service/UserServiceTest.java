package com.Rootin.domain.user.service;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
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
}
