package com.Rootin.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 로그인/회원가입 공통 토큰 응답 DTO
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    // JWT Access Token — API 호출 시 Authorization 헤더에 담아 보냄
    private String accessToken;

    // JWT Refresh Token — Access Token 만료 시 재발급에 사용
    private String refreshToken;

    // Access Token 만료 시간 (초 단위) — /자동 재발급에 활용
    private long accessTokenExpiresIn;

    // Refresh Token 만료까지 남은 시간 (초 단위) — 세션 만료 안내 등에 활용 가능
    private long refreshTokenExpiresIn;

    /**
     * 최초 가입 여부 (구글 로그인 전용)
     * true: 처음 가입한 사용자 → 프론트에서 온보딩(닉네임 설정 등) 화면으로 이동
     * false: 기존 사용자 → 바로 홈으로 이동
     * null: 이메일 로그인/회원가입 시 → JSON에서 필드 자체가 제외됨
     */
    private Boolean isNewUser;
}
