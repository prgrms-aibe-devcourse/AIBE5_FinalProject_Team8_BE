package com.Rootin.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 구글 소셜 로그인 요청 DTO
// API: [POST] /auth/google
@Getter
@NoArgsConstructor
public class GoogleLoginRequest {

    // Google OAuth 로그인 후 받는 JWT 형태의 토큰
    @NotBlank(message = "Google ID Token은 필수입니다.")
    private String idToken;
}
