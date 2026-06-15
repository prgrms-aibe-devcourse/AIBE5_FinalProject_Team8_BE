package com.Rootin.domain.auth.service;

import com.Rootin.domain.auth.dto.GoogleLoginRequest;
import com.Rootin.domain.auth.dto.TokenResponse;
import com.Rootin.domain.auth.entity.RefreshToken;
import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 구글 로그인 시 Google 프로필(name, picture) 신규 가입 반영 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceGoogleProfileTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RestTemplate restTemplate;

    private static final String FAKE_ID_TOKEN = "fake.id.token";
    private static final String GOOGLE_SUB    = "123456789012345";
    private static final String GOOGLE_EMAIL  = "test@gmail.com";
    private static final String GOOGLE_NAME   = "홍길동";
    private static final String GOOGLE_PICTURE = "https://lh3.googleusercontent.com/a/photo=s96-c";

    @BeforeEach
    void setUp() {
        given(jwtTokenProvider.createAccessToken(any(), anyString(), anyString())).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpirationInSeconds()).willReturn(3600L);
        given(jwtTokenProvider.getRefreshTokenExpirationDays()).willReturn(14L);
    }

    /** Google tokeninfo API 응답 stubbing 헬퍼 */
    private void mockGoogleTokenInfo(Map<String, Object> response) {
        given(restTemplate.getForObject(contains("tokeninfo"), eq(Map.class))).willReturn(response);
    }

    private Map<String, Object> buildGoogleResponse(String name, String picture) {
        return buildGoogleResponse(name, picture, "true");
    }

    private Map<String, Object> buildGoogleResponse(String name, String picture, String emailVerified) {
        Map<String, Object> map = new HashMap<>();
        map.put("sub", GOOGLE_SUB);
        map.put("email", GOOGLE_EMAIL);
        map.put("email_verified", emailVerified);
        if (name != null)    map.put("name", name);
        if (picture != null) map.put("picture", picture);
        return map;
    }

    private GoogleLoginRequest buildRequest() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        ReflectionTestUtils.setField(req, "idToken", FAKE_ID_TOKEN);
        return req;
    }

    private void stubNewUserSave(String nickname, String picture) {
        User savedUser = User.builder()
                .email(GOOGLE_EMAIL).nickname(nickname).profileImage(picture)
                .role(Role.USER).provider(Provider.GOOGLE).providerId(GOOGLE_SUB).point(0)
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(i -> i.getArgument(0));
    }

    // =====================================================================
    // 신규 가입 — 정상 케이스
    // =====================================================================

    @Test
    @DisplayName("신규 가입 시 Google name이 nickname, picture가 profileImage로 설정된다")
    void newUser_googleProfileApplied() {
        mockGoogleTokenInfo(buildGoogleResponse(GOOGLE_NAME, GOOGLE_PICTURE));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(GOOGLE_NAME)).willReturn(false);
        stubNewUserSave(GOOGLE_NAME, GOOGLE_PICTURE);

        TokenResponse response = authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo(GOOGLE_NAME);
        assertThat(captor.getValue().getProfileImage()).isEqualTo(GOOGLE_PICTURE);
        assertThat(response.getIsNewUser()).isTrue();
    }

    // =====================================================================
    // 신규 가입 — nickname 길이 제한 (50자 컬럼)
    // =====================================================================

    @Test
    @DisplayName("Google name이 20자 초과이면 20자로 truncate되어 nickname에 설정된다")
    void newUser_longGoogleName_truncatedTo20() {
        String longName = "이름이매우길어서스무자가넘어버리는경우입니다"; // 21자
        String truncated = longName.substring(0, 20);

        mockGoogleTokenInfo(buildGoogleResponse(longName, GOOGLE_PICTURE));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(truncated)).willReturn(false);
        stubNewUserSave(truncated, GOOGLE_PICTURE);

        authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo(truncated);
        assertThat(captor.getValue().getNickname().length()).isLessThanOrEqualTo(20);
    }

    // =====================================================================
    // 신규 가입 — nickname 중복 fallback
    // =====================================================================

    @Test
    @DisplayName("nickname 중복 시 name_sub앞4자리로 fallback된다")
    void newUser_nicknameDuplicate_fallback() {
        mockGoogleTokenInfo(buildGoogleResponse(GOOGLE_NAME, GOOGLE_PICTURE));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(GOOGLE_NAME)).willReturn(true); // 중복

        String fallback = GOOGLE_NAME + "_" + GOOGLE_SUB.substring(0, 4);
        given(userRepository.existsByNickname(fallback)).willReturn(false);
        stubNewUserSave(fallback, GOOGLE_PICTURE);

        authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo(fallback);
    }

    @Test
    @DisplayName("nickname과 fallback 모두 중복이면 user_sub뒷14자리로 설정된다")
    void newUser_nicknameBothDuplicate_fallbackToSub() {
        mockGoogleTokenInfo(buildGoogleResponse(GOOGLE_NAME, GOOGLE_PICTURE));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(GOOGLE_NAME)).willReturn(true); // 1차 중복

        String fallback = GOOGLE_NAME + "_" + GOOGLE_SUB.substring(0, 4);
        given(userRepository.existsByNickname(fallback)).willReturn(true); // 2차 중복

        // sub 뒷 14자리 사용: "123456789012345".substring(1) = "23456789012345"
        String subTail = GOOGLE_SUB.substring(Math.max(0, GOOGLE_SUB.length() - 14));
        String expectedNickname = "user_" + subTail;
        stubNewUserSave(expectedNickname, GOOGLE_PICTURE);

        authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo(expectedNickname);
        assertThat(captor.getValue().getNickname().length()).isLessThanOrEqualTo(20);
    }

    // =====================================================================
    // 신규 가입 — Google 응답에 name / picture 없는 경우
    // =====================================================================

    @Test
    @DisplayName("Google 응답에 name이 없으면 user_sub앞8자리로 nickname이 설정된다")
    void newUser_noGoogleName_fallbackToSub() {
        String expectedNickname = "user_" + GOOGLE_SUB.substring(0, 8);

        mockGoogleTokenInfo(buildGoogleResponse(null, GOOGLE_PICTURE));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(expectedNickname)).willReturn(false);
        stubNewUserSave(expectedNickname, GOOGLE_PICTURE);

        authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo(expectedNickname);
    }

    @Test
    @DisplayName("Google 응답에 picture가 없으면 profileImage가 null로 저장된다")
    void newUser_noGooglePicture_profileImageIsNull() {
        mockGoogleTokenInfo(buildGoogleResponse(GOOGLE_NAME, null));
        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(GOOGLE_NAME)).willReturn(false);
        stubNewUserSave(GOOGLE_NAME, null);

        authService.googleLogin(buildRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProfileImage()).isNull();
    }

    // =====================================================================
    // email_verified 검증
    // =====================================================================

    @Test
    @DisplayName("email_verified가 false이면 예외가 발생한다")
    void emailNotVerified_throwsException() {
        mockGoogleTokenInfo(buildGoogleResponse(GOOGLE_NAME, GOOGLE_PICTURE, "false"));

        assertThatThrownBy(() -> authService.googleLogin(buildRequest()))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("이메일 인증이 완료되지 않은");
    }

    @Test
    @DisplayName("email_verified 키 자체가 없으면(null) 예외가 발생한다")
    void emailVerifiedMissing_throwsException() {
        Map<String, Object> responseWithoutVerified = buildGoogleResponse(GOOGLE_NAME, GOOGLE_PICTURE, "true");
        responseWithoutVerified.remove("email_verified"); // 키 제거
        mockGoogleTokenInfo(responseWithoutVerified);

        assertThatThrownBy(() -> authService.googleLogin(buildRequest()))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("이메일 인증이 완료되지 않은");
    }

    @Test
    @DisplayName("email_verified가 Boolean true이면 정상 가입된다")
    void emailVerifiedAsBoolean_success() {
        Map<String, Object> response = new HashMap<>();
        response.put("sub", GOOGLE_SUB);
        response.put("email", GOOGLE_EMAIL);
        response.put("email_verified", Boolean.TRUE); // Boolean 타입
        response.put("name", GOOGLE_NAME);
        response.put("picture", GOOGLE_PICTURE);
        mockGoogleTokenInfo(response);

        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.empty());
        given(userRepository.existsByEmail(GOOGLE_EMAIL)).willReturn(false);
        given(userRepository.existsByNickname(GOOGLE_NAME)).willReturn(false);
        stubNewUserSave(GOOGLE_NAME, GOOGLE_PICTURE);

        TokenResponse result = authService.googleLogin(buildRequest());
        assertThat(result.getIsNewUser()).isTrue();
    }

    @Test
    @DisplayName("email_verified가 Boolean false이면 예외가 발생한다")
    void emailVerifiedAsBooleanFalse_throwsException() {
        Map<String, Object> response = new HashMap<>();
        response.put("sub", GOOGLE_SUB);
        response.put("email", GOOGLE_EMAIL);
        response.put("email_verified", Boolean.FALSE); // Boolean 타입
        response.put("name", GOOGLE_NAME);
        response.put("picture", GOOGLE_PICTURE);
        mockGoogleTokenInfo(response);

        assertThatThrownBy(() -> authService.googleLogin(buildRequest()))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("이메일 인증이 완료되지 않은");
    }

    // =====================================================================
    // 기존 유저 재로그인 — 프로필 업데이트 없음
    // =====================================================================

    @Test
    @DisplayName("기존 유저 재로그인 시 profileImage가 변경되지 않는다")
    void existingUser_profileNotUpdatedOnReLogin() {
        mockGoogleTokenInfo(buildGoogleResponse("변경된이름", "https://new-picture.url"));

        User existingUser = User.builder()
                .email(GOOGLE_EMAIL).nickname("기존닉네임").profileImage("https://old-picture.url")
                .role(Role.USER).provider(Provider.GOOGLE).providerId(GOOGLE_SUB).point(0)
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        given(userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, GOOGLE_SUB)).willReturn(Optional.of(existingUser));
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(i -> i.getArgument(0));

        TokenResponse response = authService.googleLogin(buildRequest());

        verify(userRepository, never()).save(any(User.class));
        assertThat(response.getIsNewUser()).isFalse();
        assertThat(existingUser.getProfileImage()).isEqualTo("https://old-picture.url");
    }
}
