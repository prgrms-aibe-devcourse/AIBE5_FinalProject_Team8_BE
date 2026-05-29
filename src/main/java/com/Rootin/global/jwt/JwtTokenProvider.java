package com.Rootin.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 생성, 검증, 파싱 유틸리티
@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:1800000}")   // 기본 30분 (밀리초)
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:1209600000}") // 기본 14일 (밀리초)
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // =====================================================================
    // 토큰 생성
    // =====================================================================

    /**
     * Access Token 생성 — subject: email, claim: userId, role
     */
    public String createAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성 — subject: email만 포함 (최소 정보)
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // =====================================================================
    // 토큰 파싱
    // =====================================================================

    /**
     * 토큰에서 email(subject) 추출
     */
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // =====================================================================
    // 토큰 검증
    // =====================================================================

    /**
     * 토큰 유효성 검증 — 서명, 만료, 형식 검사
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰입니다.");
        } catch (SecurityException e) {
            log.warn("JWT 서명 검증에 실패했습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다.");
        }
        return false;
    }

    // =====================================================================
    // 유틸리티
    // =====================================================================

    /**
     * Access Token 만료 시간(초) 반환 — 응답용
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Refresh Token 만료 일수 반환 — RefreshToken 엔티티 생성용
     */
    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpiration / (1000 * 60 * 60 * 24);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
