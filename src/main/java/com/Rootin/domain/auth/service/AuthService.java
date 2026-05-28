package com.Rootin.domain.auth.service;

import com.Rootin.domain.auth.dto.*;
import com.Rootin.domain.auth.entity.RefreshToken;
import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 인증/회원 비즈니스 로직 서비스
 *
 * API 설계서 1. 인증/회원 도메인의 핵심 로직을 담당한다.
 * 컨트롤러는 이 서비스를 호출만 하고, 실제 비즈니스 로직은 모두 여기에 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // =====================================================================
    // 1. 이메일 회원가입 — POST /api/v1/auth/signup
    // =====================================================================

    /**
     * 이메일 회원가입
     *
     * 처리 흐름:
     *   1. 이메일 중복 검사
     *   2. 닉네임 중복 검사
     *   3. 비밀번호를 BCrypt로 암호화
     *   4. User 엔티티 생성 및 저장
     *   5. JWT Access Token + Refresh Token 발급
     *   6. Refresh Token을 DB에 저장
     *   7. TokenResponse 반환
     *
     * @param request 회원가입 요청 (email, password, nickname)
     * @return TokenResponse (accessToken, refreshToken, accessTokenExpiresIn)
     */
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 닉네임 중복 검사
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3~4. 비밀번호 암호화 후 User 생성 및 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .point(0)
                .build();

        userRepository.save(user);

        // 5~7. 토큰 발급 및 반환
        return issueTokens(user, null);
    }

    // =====================================================================
    // 2. 이메일 로그인 — POST /api/v1/auth/login
    // =====================================================================

    /**
     * 이메일 로그인
     *
     * 처리 흐름:
     *   1. AuthenticationManager에게 email/password 검증을 위임
     *      → 내부적으로 CustomUserDetailsService.loadUserByUsername() 호출
     *      → BCrypt 비밀번호 비교
     *      → 실패 시 Spring Security가 BadCredentialsException 발생
     *   2. 인증 성공 시 User 엔티티 조회
     *   3. 기존 Refresh Token 삭제 (중복 로그인 방지)
     *   4. 새 JWT Access Token + Refresh Token 발급
     *
     * @param request 로그인 요청 (email, password)
     * @return TokenResponse
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. Spring Security 인증 — 실패 시 BadCredentialsException 발생
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. 인증 성공 → User 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 기존 Refresh Token 삭제 (이 사용자의 이전 세션 무효화)
        refreshTokenRepository.deleteByUserId(user.getId());

        // 4. 새 토큰 발급 및 반환
        return issueTokens(user, null);
    }

    // =====================================================================
    // 3. 구글 소셜 로그인 — POST /api/v1/auth/google
    // =====================================================================

    /**
     * 구글 소셜 로그인
     *
     * 처리 흐름:
     *   1. Google tokeninfo API로 idToken 검증 → email, sub(Google 고유 ID) 추출
     *   2. provider=GOOGLE + providerId=sub 으로 기존 사용자 조회
     *   3-A. 기존 사용자 → 로그인 처리 (isNewUser = false)
     *   3-B. 신규 사용자 → 자동 회원가입 후 로그인 (isNewUser = true)
     *        → 프론트에서 isNewUser=true를 받으면 온보딩(닉네임 설정) 화면으로 이동
     *   4. JWT 토큰 발급
     *
     * @param request 구글 로그인 요청 (idToken)
     * @return TokenResponse (isNewUser 포함)
     */
    @Transactional
    public TokenResponse googleLogin(GoogleLoginRequest request) {
        // 1. Google ID Token 검증 → 사용자 정보 추출
        Map<String, Object> googleUser = verifyGoogleToken(request.getIdToken());
        String email = (String) googleUser.get("email");
        String sub = (String) googleUser.get("sub"); // Google 고유 사용자 ID

        // 2. 기존 사용자 조회
        boolean isNewUser = false;
        User user = userRepository.findByProviderAndProviderId(Provider.GOOGLE, sub)
                .orElse(null);

        // 3. 신규 사용자 → 자동 회원가입
        if (user == null) {
            isNewUser = true;
            user = User.builder()
                    .email(email)
                    .nickname("user_" + sub.substring(0, 8)) // 임시 닉네임 (온보딩에서 변경)
                    .role(Role.USER)
                    .provider(Provider.GOOGLE)
                    .providerId(sub)
                    .point(0)
                    .build();
            userRepository.save(user);
        }

        // 4. 기존 Refresh Token 삭제 후 새 토큰 발급
        refreshTokenRepository.deleteByUserId(user.getId());
        return issueTokens(user, isNewUser);
    }

    // =====================================================================
    // 4. Access Token 재발급 — POST /api/v1/auth/reissue
    // =====================================================================

    /**
     * Access Token 재발급
     *
     * 처리 흐름:
     *   1. 클라이언트가 보낸 Refresh Token으로 DB 조회
     *   2. 토큰 만료 여부 확인
     *   3. 연결된 User 정보로 새 Access Token 발급
     *   4. 기존 Refresh Token은 유지 (Refresh Token Rotation 미적용)
     *
     * Refresh Token이 없거나 만료되었으면 → 재로그인 필요 (예외 발생)
     *
     * @param refreshTokenValue 클라이언트가 보낸 Refresh Token 문자열
     * @return TokenResponse (새 accessToken + 기존 refreshToken)
     */
    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {
        // 1. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Refresh Token입니다."));

        // 2. 만료 확인
        if (refreshToken.isExpired()) {
            // 만료된 토큰은 삭제하고 재로그인 유도
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 다시 로그인해 주세요.");
        }

        // 3. 연결된 User 정보로 새 Access Token 발급
        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // 4. 기존 Refresh Token 유지, 새 Access Token만 반환
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue) // 기존 Refresh Token 그대로
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .build();
    }

    // =====================================================================
    // 5. 로그아웃 — POST /api/v1/auth/logout
    // =====================================================================

    /**
     * 로그아웃
     *
     * 처리 흐름:
     *   1. 해당 사용자의 모든 Refresh Token을 DB에서 삭제
     *   2. Access Token은 서버에서 무효화할 수 없으므로 (Stateless JWT),
     *      프론트엔드에서 저장된 토큰을 삭제하는 것으로 로그아웃 완료
     *
     * @param userId 로그아웃할 사용자의 ID (JWT에서 추출)
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // =====================================================================
    // 6. 닉네임 중복 확인 — GET /api/v1/auth/check-nickname
    // =====================================================================

    /**
     * 닉네임 중복 확인
     *
     * @param nickname 확인할 닉네임
     * @return true: 사용 가능 / false: 이미 사용 중
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // =====================================================================
    // Private 헬퍼 메서드
    // =====================================================================

    /**
     * JWT Access Token + Refresh Token 발급 공통 메서드
     *
     * 회원가입, 로그인, 구글 로그인에서 공통으로 사용한다.
     * Refresh Token은 DB에 저장하여 재발급/로그아웃 시 관리한다.
     *
     * @param user      토큰을 발급할 대상 사용자
     * @param isNewUser 구글 로그인 시 최초 가입 여부 (이메일 로그인 시 null)
     * @return TokenResponse
     */
    private TokenResponse issueTokens(User user, Boolean isNewUser) {
        // Access Token 생성 — userId, email, role 포함
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Refresh Token 생성 — email만 포함 (최소 정보)
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        // Refresh Token을 DB에 저장 (재발급/로그아웃 시 조회/삭제용)
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(java.time.LocalDateTime.now()
                        .plusDays(jwtTokenProvider.getRefreshTokenExpirationDays()))
                .build();
        refreshTokenRepository.save(refreshToken);

        // 응답 생성
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * Google ID Token 검증
     *
     * Google의 tokeninfo 엔드포인트에 idToken을 보내서 검증한다.
     * 검증 성공 시 email, sub(Google 고유 ID) 등의 사용자 정보를 반환받는다.
     * 검증 실패 시 Google API가 에러를 반환하므로 예외가 발생한다.
     *
     * @param idToken 프론트에서 Google SDK로 받은 ID Token
     * @return Google 사용자 정보 (email, sub 등)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 Google ID Token입니다.");
        }
    }
}