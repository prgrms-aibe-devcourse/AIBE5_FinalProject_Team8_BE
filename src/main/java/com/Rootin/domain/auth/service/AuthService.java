package com.Rootin.domain.auth.service;

import com.Rootin.domain.auth.dto.*;
import com.Rootin.domain.auth.entity.RefreshToken;
import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
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

    // nickname 생성 관련 상수
    private static final int NICKNAME_MAX_LENGTH = 20;    // Google name truncate 최대 길이
    private static final int SUB_PREFIX_LENGTH   = 8;     // name 없을 때 user_sub앞8자리
    private static final int SUB_SUFFIX_LENGTH   = 4;     // 중복 fallback 시 sub앞4자리

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

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
        // 1. 이메일 중복 검사 — 탈퇴 계정이면 즉시 영구 삭제 후 재가입 허용
        userRepository.findByEmailWithLock(request.getEmail()).ifPresent(existing -> {
            if (!existing.isEnabled()) {
                deleteUserAndTokens(existing);
            } else {
                throw CustomException.badRequest("이미 사용 중인 이메일입니다.");
            }
        });

        // 2. 닉네임 중복 검사
        if (userRepository.existsByNickname(request.getNickname())) {
            throw CustomException.badRequest("이미 사용 중인 닉네임입니다.");
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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "탈퇴한 계정입니다.");
        } catch (BadCredentialsException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 2. 인증 성공 → User 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

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
        if (email == null || sub == null) {
            throw CustomException.badRequest("유효하지 않은 Google ID Token입니다.");
        }
        // tokeninfo REST → String "true"/"false", JWT payload → Boolean. 양쪽 모두 방어
        Object rawVerified = googleUser.get("email_verified");
        if (!Boolean.TRUE.equals(rawVerified) && !"true".equals(rawVerified)) {
            throw CustomException.badRequest("이메일 인증이 완료되지 않은 Google 계정입니다.");
        }

        // Google 프로필 정보 (신규 가입 시에만 사용)
        String googleName = (String) googleUser.get("name");
        String googlePicture = (String) googleUser.get("picture");

        // 2. 기존 사용자 조회 (비관적 락 — 동시 재가입 요청 방지)
        boolean isNewUser = false;
        User user = userRepository.findByProviderAndProviderIdWithLock(Provider.GOOGLE, sub)
                .orElse(null);

        // 탈퇴한 계정 → 즉시 영구 삭제 후 신규 가입 허용
        if (user != null && !user.isEnabled()) {
            deleteUserAndTokens(user);
            user = null;
        }

        // 3. 신규 사용자 → 자동 회원가입
        if (user == null) {
            if (userRepository.existsByEmail(email)) {
                throw CustomException.badRequest("이미 다른 방식으로 가입된 이메일입니다.");
            }

            String nickname = generateUniqueNickname(googleName, sub);

            isNewUser = true;
            user = User.builder()
                    .email(email)
                    .nickname(nickname)
                    .profileImage(googlePicture) // Google 프로필 이미지 URL (없으면 null)
                    .role(Role.USER)
                    .provider(Provider.GOOGLE)
                    .providerId(sub)
                    .point(0)
                    .build();
            try {
                userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                // nickname/email/providerId 중 하나의 unique 제약 위반 (동시 요청 레이스 컨디션)
                throw CustomException.badRequest("가입 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
            }
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
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw CustomException.badRequest("Refresh Token은 필수입니다.");
        }

        // 1. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."));

        // 2. 만료 확인
        if (refreshToken.isExpired()) {
            // 만료된 토큰은 삭제하고 재로그인 유도
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다. 다시 로그인해 주세요.");
        }

        // 3. 기존 Refresh Token 삭제 (Rotation: 재사용 불가)
        refreshTokenRepository.delete(refreshToken);

        // 4. 연결된 User로 Access Token + Refresh Token 모두 재발급
        User user = refreshToken.getUser();
        return issueTokens(user, null);
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
     * 탈퇴 계정의 리프레시 토큰 및 사용자 데이터 영구 삭제
     *
     * googleLogin, signup 양쪽에서 공통으로 사용.
     * 호출하는 메서드(@Transactional)의 트랜잭션 안에서 실행되어 원자성이 보장된다.
     */
    private void deleteUserAndTokens(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    /**
     * Google name 기반 고유 닉네임 생성
     *
     * 우선순위:
     *   1. googleName (최대 NICKNAME_MAX_LENGTH자 truncate)
     *   2. 중복 시 → baseNickname + "_" + sub앞 SUB_SUFFIX_LENGTH자
     *   3. 2도 중복 시 → "user_" + sub 전체 (Google sub는 고유값이므로 최종 보장)
     *   name이 없으면 "user_" + sub앞 SUB_PREFIX_LENGTH자를 base로 사용.
     */
    private String generateUniqueNickname(String googleName, String sub) {
        String baseNickname = (googleName != null && !googleName.isBlank())
                ? googleName.substring(0, Math.min(googleName.length(), NICKNAME_MAX_LENGTH))
                : "user_" + sub.substring(0, Math.min(sub.length(), SUB_PREFIX_LENGTH));

        if (!userRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }

        // baseNickname max 20자 + "_" + 4자 = max 25자 → 50자 컬럼 이내 보장됨
        String fallback = baseNickname + "_" + sub.substring(0, Math.min(sub.length(), SUB_SUFFIX_LENGTH));
        if (!userRepository.existsByNickname(fallback)) {
            return fallback;
        }

        // sub 앞자리는 발급 시기 기반으로 겹칠 수 있으므로 뒷 14자리 사용 → max 19자 (20자 이내)
        String subTail = sub.substring(Math.max(0, sub.length() - 14));
        String subNickname = "user_" + subTail;
        return subNickname.substring(0, Math.min(subNickname.length(), NICKNAME_MAX_LENGTH));
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
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw CustomException.badRequest("유효하지 않은 Google ID Token입니다.");
            }
            return response;
        } catch (Exception e) {
            throw CustomException.badRequest("유효하지 않은 Google ID Token입니다.");
        }
    }
}
