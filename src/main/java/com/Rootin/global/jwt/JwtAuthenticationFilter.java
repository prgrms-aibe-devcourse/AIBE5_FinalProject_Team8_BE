package com.Rootin.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 JWT를 검사하고 SecurityContext에 인증 정보를 설정하는 필터.
 *
 * DB 조회 없이 Access Token 클레임(userId, email, role)만으로 {@link JwtUserDetails}를 구성한다.
 * 기존에 매 요청마다 발생하던 users 테이블 SELECT가 제거된다.
 *
 * 보안 정책상 access token은 만료 시각까지 유효한 bearer credential로 취급한다.
 * 따라서 사용자 삭제/비활성화 직후 즉시 차단이 필요해지면, 이 필터에 DB 조회를 되돌리기보다
 * tokenVersion, blacklist, 짧은 access token TTL 같은 별도 폐기 전략을 함께 도입해야 한다.
 *
 * 만료 토큰의 경우 요청 속성 "TOKEN_EXPIRED"를 true로 설정하여
 * SecurityConfig의 authenticationEntryPoint에서 401 응답 코드를 구분한다.
 */
@Slf4j
@Component
@ConditionalOnBean(JwtTokenProvider.class)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);

                // DB 조회 없이 클레임에서 바로 인증 객체 구성
                Long   userId = extractUserId(claims);
                String email  = extractSubject(claims);
                String role   = extractRequiredTextClaim(claims, "role");

                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                JwtUserDetails principal =
                        new JwtUserDetails(userId, email, authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute("TOKEN_EXPIRED", true);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("유효하지 않은 JWT 토큰입니다.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long extractUserId(Claims claims) {
        Object claim = claims.get("userId");
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value && StringUtils.hasText(value)) {
            return Long.parseLong(value);
        }
        throw new IllegalArgumentException("JWT userId claim is missing.");
    }

    private String extractSubject(Claims claims) {
        String subject = claims.getSubject();
        if (StringUtils.hasText(subject)) {
            return subject;
        }
        throw new IllegalArgumentException("JWT subject claim is missing.");
    }

    private String extractRequiredTextClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (StringUtils.hasText(value)) {
            return value;
        }
        throw new IllegalArgumentException("JWT " + name + " claim is missing.");
    }

    /**
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 토큰 반환
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
