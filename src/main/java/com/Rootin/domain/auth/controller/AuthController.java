package com.Rootin.domain.auth.controller;

import com.Rootin.domain.auth.dto.*;
import com.Rootin.domain.auth.service.AuthService;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증/회원 API 컨트롤러
 *
 * API 설계서 1. 인증/회원 도메인의 엔드포인트를 정의한다.
 * 컨트롤러는 요청 검증과 응답 포맷만 담당하고,
 * 실제 비즈니스 로직은 AuthService에 위임한다.
 *
 * 엔드포인트 목록:
 *   POST /api/v1/auth/signup          이메일 회원가입
 *   POST /api/v1/auth/login           이메일 로그인
 *   POST /api/v1/auth/google          구글 소셜 로그인
 *   POST /api/v1/auth/reissue         Access Token 재발급
 *   POST /api/v1/auth/logout          로그아웃
 *   GET  /api/v1/auth/check-nickname  닉네임 중복 확인
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================================
    // 1. 이메일 회원가입
    // =====================================================================

    /**
     * POST /api/v1/auth/signup
     *
     * 인증 필요: ❌
     * 요청: { email, password, nickname }
     * 응답: { accessToken, refreshToken, accessTokenExpiresIn }
     * 상태코드: 201 Created
     *
     * @Valid가 SignupRequest의 유효성 검사 어노테이션을 실행한다.
     * 검사 실패 시 MethodArgumentNotValidException → 400 Bad Request
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        TokenResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    // =====================================================================
    // 2. 이메일 로그인
    // =====================================================================

    /**
     * POST /api/v1/auth/login
     *
     * 인증 필요: ❌
     * 요청: { email, password }
     * 응답: { accessToken, refreshToken, accessTokenExpiresIn }
     * 상태코드: 200 OK
     *
     * 비밀번호 불일치 시 AuthService에서 BadCredentialsException 발생 → 401
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    // =====================================================================
    // 3. 구글 소셜 로그인
    // =====================================================================

    /**
     * POST /api/v1/auth/google
     *
     * 인증 필요: ❌
     * 요청: { idToken }
     * 응답: { accessToken, refreshToken, accessTokenExpiresIn, isNewUser }
     * 상태코드: 200 OK
     *
     * isNewUser=true → 프론트에서 온보딩(닉네임 설정) 화면으로 분기
     * isNewUser=false → 바로 홈 화면으로 이동
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<TokenResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        TokenResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success("구글 로그인 성공", response));
    }

    // =====================================================================
    // 4. Access Token 재발급
    // =====================================================================

    /**
     * POST /api/v1/auth/reissue
     *
     * 인증 필요: ❌ (Access Token이 만료된 상태에서 호출하므로)
     * 요청: { refreshToken }
     * 응답: { accessToken, refreshToken, accessTokenExpiresIn }
     * 상태코드: 200 OK
     *
     * Refresh Token이 만료되었거나 DB에 없으면 → 401 (재로그인 필요)
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @RequestBody Map<String, String> request
    ) {
        String refreshToken = request.get("refreshToken");
        TokenResponse response = authService.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    // =====================================================================
    // 5. 로그아웃
    // =====================================================================

    /**
     * POST /api/v1/auth/logout
     *
     * 인증 필요: ✅ (로그인된 사용자만 로그아웃 가능)
     * 요청: 없음 (JWT에서 사용자 정보 추출)
     * 응답: 없음
     * 상태코드: 200 OK
     *
     * @AuthenticationPrincipal은 SecurityContext에서 인증된 User 객체를 주입한다.
     * JwtAuthenticationFilter에서 설정한 UserDetails(=User)가 여기로 들어온다.
     *
     * 서버에서는 Refresh Token을 DB에서 삭제하고,
     * 프론트에서는 저장된 Access Token/Refresh Token을 삭제해야 로그아웃이 완료된다.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User user
    ) {
        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

    // =====================================================================
    // 6. 닉네임 중복 확인
    // =====================================================================

    /**
     * GET /api/v1/auth/check-nickname?nickname=루틴
     *
     * 인증 필요: ❌
     * 요청: Query Parameter (nickname)
     * 응답: { available: true/false }
     * 상태코드: 200 OK
     *
     * 회원가입 폼에서 닉네임 입력 시 실시간 중복 확인에 사용된다.
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(
            @RequestParam String nickname
    ) {
        boolean available = authService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(
                ApiResponse.success("닉네임 확인 완료", Map.of("available", available))
        );
    }
}