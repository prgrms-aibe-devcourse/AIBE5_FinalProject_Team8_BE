package com.Rootin.global.jwt;

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
 * DB 조회 없이 Access Token 클레임(userId, email, role)만으로
 * {@link JwtUserDetails}를 구성한다.
 * 기존에 매 요청마다 발생하던 users 테이블 SELECT가 제거된다.
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
            // 만료 여부를 먼저 확인해 응답 코드 구분용 속성 설정
            if (jwtTokenProvider.isExpiredToken(token)) {
                request.setAttribute("TOKEN_EXPIRED", true);
            } else if (jwtTokenProvider.validateToken(token)) {
                // DB 조회 없이 클레임에서 바로 인증 객체 구성
                Long   userId = jwtTokenProvider.getUserId(token);
                String email  = jwtTokenProvider.getEmail(token);
                String role   = jwtTokenProvider.getRole(token);

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
            }
        }

        filterChain.doFilter(request, response);
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
