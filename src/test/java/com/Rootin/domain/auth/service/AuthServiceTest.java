package com.Rootin.domain.auth.service;

import com.Rootin.domain.auth.dto.TokenResponse;
import com.Rootin.domain.auth.entity.RefreshToken;
import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RestTemplate restTemplate;

    @Test
    @DisplayName("Refresh Token 재발급 시 기존 Refresh Token을 유지하고 Access Token만 새로 발급한다")
    void reissue_keepsRefreshTokenAndIssuesNewAccessToken() {
        // given
        String refreshTokenValue = "refresh-token";
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken(user, refreshTokenValue, LocalDateTime.now().plusDays(7));

        given(refreshTokenRepository.findByToken(refreshTokenValue)).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER")).willReturn("new-access-token");
        given(jwtTokenProvider.getAccessTokenExpirationInSeconds()).willReturn(1800L);

        // when
        TokenResponse response = authService.reissue(refreshTokenValue);

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.getAccessTokenExpiresIn()).isEqualTo(1800L);

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token으로 재발급을 요청하면 401 예외가 발생한다")
    void reissue_invalidRefreshToken_throwsUnauthorized() {
        // given
        given(refreshTokenRepository.findByToken("invalid-refresh-token")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue("invalid-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급을 요청하면 401 예외가 발생한다")
    void reissue_expiredRefreshToken_throwsUnauthorized() {
        // given
        User user = buildUser();
        RefreshToken expiredToken = buildRefreshToken(user, "expired-refresh-token", LocalDateTime.now().minusDays(1));
        given(refreshTokenRepository.findByToken("expired-refresh-token")).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue("expired-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그아웃하면 해당 사용자의 Refresh Token을 삭제한다")
    void logout_deletesUserRefreshTokens() {
        // when
        authService.logout(1L);

        // then
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    private User buildUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded-password")
                .nickname("테스터")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .point(0)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private RefreshToken buildRefreshToken(User user, String token, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }
}
