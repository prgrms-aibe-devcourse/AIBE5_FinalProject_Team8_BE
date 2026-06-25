package com.Rootin.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Claims claims;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 JWT 클레임이면 DB 조회 없이 인증 객체를 생성한다")
    void doFilter_validClaims_setsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();

        given(jwtTokenProvider.parseClaims("valid-token")).willReturn(claims);
        given(claims.get("userId")).willReturn(1L);
        given(claims.getSubject()).willReturn("user@test.com");
        given(claims.get("role", String.class)).willReturn("USER");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(((JwtUserDetails) authentication.getPrincipal()).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("role 클레임이 없으면 ROLE_null 인증을 만들지 않는다")
    void doFilter_missingRole_doesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();

        given(jwtTokenProvider.parseClaims("valid-token")).willReturn(claims);
        given(claims.get("userId")).willReturn(1L);
        given(claims.getSubject()).willReturn("user@test.com");
        given(claims.get("role", String.class)).willReturn(null);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("만료 JWT는 TOKEN_EXPIRED 속성만 남기고 인증하지 않는다")
    void doFilter_expiredToken_marksRequestAttribute() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();

        given(jwtTokenProvider.parseClaims("valid-token"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("TOKEN_EXPIRED")).isEqualTo(true);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("서명 검증 실패 JWT는 인증 객체를 만들지 않는다")
    void doFilter_invalidSignature_doesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();

        given(jwtTokenProvider.parseClaims("valid-token"))
                .willThrow(new SignatureException("invalid signature"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("TOKEN_EXPIRED")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("클레임 추출 중 JWT 예외가 발생하면 인증 객체를 만들지 않는다")
    void doFilter_invalidClaim_doesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();

        given(jwtTokenProvider.parseClaims("valid-token")).willReturn(claims);
        given(claims.get("userId")).willReturn(1L);
        given(claims.getSubject()).willReturn("user@test.com");
        given(claims.get("role", String.class)).willThrow(new JwtException("invalid claim"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("TOKEN_EXPIRED")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }
}
