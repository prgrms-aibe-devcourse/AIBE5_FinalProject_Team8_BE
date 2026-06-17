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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
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
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-06-16T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("Refresh Token 재발급 시 기존 토큰을 회전하고 새 Access Token과 Refresh Token을 발급한다")
    void reissue_rotatesRefreshTokenAndIssuesNewTokens() {
        // given
        String refreshTokenValue = "refresh-token";
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken(user, refreshTokenValue, now().plusDays(7));

        given(refreshTokenRepository.findByTokenForUpdate(refreshTokenValue)).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER")).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken("test@example.com")).willReturn("new-refresh-token");
        given(jwtTokenProvider.getAccessTokenExpirationInSeconds()).willReturn(1800L);
        given(jwtTokenProvider.getRefreshTokenExpirationInSeconds()).willReturn(14L * 24 * 60 * 60);
        ReflectionTestUtils.setField(authService, "refreshTokenRotationGraceSeconds", 30L);

        // when
        TokenResponse response = authService.reissue(refreshTokenValue);

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getAccessTokenExpiresIn()).isEqualTo(1800L);
        assertThat(response.getRefreshTokenExpiresIn()).isEqualTo(14L * 24 * 60 * 60);
        assertThat(response.getIsNewUser()).isNull();
        assertThat(refreshToken.isRotated()).isTrue();
        assertThat(refreshToken.getReplacementToken()).isEqualTo("new-refresh-token");
        assertThat(refreshToken.getGraceExpiresAt()).isNotNull();

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        ArgumentCaptor<RefreshToken> replacementCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(replacementCaptor.capture());
        List<RefreshToken> savedTokens = replacementCaptor.getAllValues();
        assertThat(savedTokens).contains(refreshToken);
        assertThat(savedTokens.get(1).getToken()).isEqualTo("new-refresh-token");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("비어있는 Refresh Token으로 재발급을 요청하면 400 예외가 발생한다")
    void reissue_blankRefreshToken_throwsBadRequest(String refreshTokenValue) {
        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshTokenValue))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST)
                );

        verify(refreshTokenRepository, never()).findByTokenForUpdate(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token으로 재발급을 요청하면 401 예외가 발생한다")
    void reissue_invalidRefreshToken_throwsUnauthorized() {
        // given
        given(refreshTokenRepository.findByTokenForUpdate("invalid-refresh-token")).willReturn(Optional.empty());

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
        RefreshToken expiredToken = buildRefreshToken(user, "expired-refresh-token", now().minusDays(1));
        given(refreshTokenRepository.findByTokenForUpdate("expired-refresh-token")).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue("expired-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("회전된 Refresh Token이 유예 시간 안에 다시 들어오면 같은 대체 토큰을 반환한다")
    void reissue_rotatedRefreshTokenWithinGrace_returnsReplacementToken() {
        // given
        LocalDateTime now = now();
        User user = buildUser();
        RefreshToken rotatedToken = buildRefreshToken(user, "old-refresh-token", now.plusDays(7));
        rotatedToken.rotateTo("new-refresh-token", now.minusSeconds(1), now.plusSeconds(20));
        RefreshToken replacementToken = buildRefreshToken(user, "new-refresh-token", now.plusDays(14));

        given(refreshTokenRepository.findByTokenForUpdate("old-refresh-token")).willReturn(Optional.of(rotatedToken));
        given(refreshTokenRepository.findByToken("new-refresh-token")).willReturn(Optional.of(replacementToken));
        given(refreshTokenRepository.findByTokenForUpdate("new-refresh-token")).willReturn(Optional.of(replacementToken));
        given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER")).willReturn("new-access-token");
        given(jwtTokenProvider.getAccessTokenExpirationInSeconds()).willReturn(1800L);

        // when
        TokenResponse response = authService.reissue("old-refresh-token");

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getRefreshTokenExpiresIn()).isEqualTo(14L * 24 * 60 * 60);
        assertThat(response.getIsNewUser()).isNull();

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("회전된 대체 Refresh Token도 유예 시간 안에 다시 회전된 경우 최종 활성 토큰을 따라간다")
    void reissue_rotatedReplacementTokenWithinGrace_returnsFinalActiveToken() {
        // given
        LocalDateTime now = now();
        User user = buildUser();
        RefreshToken oldToken = buildRefreshToken(user, "old-refresh-token", now.plusDays(7));
        oldToken.rotateTo("new-refresh-token", now.minusSeconds(2), now.plusSeconds(20));
        RefreshToken newToken = buildRefreshToken(user, "new-refresh-token", now.plusDays(14));
        newToken.rotateTo("final-refresh-token", now.minusSeconds(1), now.plusSeconds(20));
        RefreshToken finalToken = buildRefreshToken(user, "final-refresh-token", now.plusDays(14));

        given(refreshTokenRepository.findByTokenForUpdate("old-refresh-token")).willReturn(Optional.of(oldToken));
        given(refreshTokenRepository.findByToken("new-refresh-token")).willReturn(Optional.of(newToken));
        given(refreshTokenRepository.findByToken("final-refresh-token")).willReturn(Optional.of(finalToken));
        given(refreshTokenRepository.findByTokenForUpdate("final-refresh-token")).willReturn(Optional.of(finalToken));
        given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER")).willReturn("new-access-token");
        given(jwtTokenProvider.getAccessTokenExpirationInSeconds()).willReturn(1800L);

        // when
        TokenResponse response = authService.reissue("old-refresh-token");

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("final-refresh-token");
        assertThat(response.getIsNewUser()).isNull();

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("회전된 Refresh Token의 유예 시간이 지나면 401 예외가 발생한다")
    void reissue_rotatedRefreshTokenAfterGrace_throwsUnauthorized() {
        // given
        LocalDateTime now = now();
        User user = buildUser();
        RefreshToken rotatedToken = buildRefreshToken(user, "old-refresh-token", now.plusDays(7));
        rotatedToken.rotateTo("new-refresh-token", now.minusMinutes(1), now.minusSeconds(1));
        given(refreshTokenRepository.findByTokenForUpdate("old-refresh-token")).willReturn(Optional.of(rotatedToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
