package com.Rootin.global.filter;

import com.Rootin.global.config.RateLimitConfig;
import com.Rootin.global.exception.ErrorCode;
import com.Rootin.global.jwt.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    // 사용자별 버킷 저장소 (MVP 스케일 기준 인메모리로 충분)
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/ai/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않은 요청은 Security 레이어에서 처리
        if (auth == null || !(auth.getPrincipal() instanceof JwtUserDetails userDetails)) {
            chain.doFilter(request, response);
            return;
        }

        Long userId = userDetails.getUserId();
        Bucket bucket = buckets.computeIfAbsent(userId, id -> rateLimitConfig.newAiBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "success", false,
                                    "message", ErrorCode.RATE_LIMIT_EXCEEDED.getMessage(),
                                    "code", ErrorCode.RATE_LIMIT_EXCEEDED.name()
                            )
                    )
            );
        }
    }
}
