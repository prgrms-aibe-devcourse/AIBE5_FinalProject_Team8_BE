package com.Rootin.global.config;

import com.Rootin.global.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * TODO [로그인 담당자]: JWT 필터 추가 예정
 * - JwtAuthenticationFilter를 addFilterBefore()로 등록
 * - 공개 경로(회원가입, 로그인 등) permitAll() 추가
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/test", "/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // 사용자 인증을 발급해주는 API 경로
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/google",
                                "/auth/reissue",
                                "auth/check-nickname"
                        ).permitAll()

                        .requestMatchers("/tils/**").permitAll()
                        .requestMatchers("/til-templates/**").permitAll()
                        // FIXME [보안 경고]: 현재 JWT 로그인 개발 단계 이전이므로, 화분 API(/api/pots/**)를 임시로 전체 허용(permitAll)해 두었습니다.
                        // 이는 테스트를 위한 일시적인 조치이며, 프로덕션 배포 전 반드시 적절한 인증 필터와 함께 인증된 권한(authenticated)을 요구하도록 정책을 전환해야 합니다.
                        .requestMatchers("/api/pots/**").permitAll()

                        // 인증(JWT 발급등)이 필요한 경로
                        .requestMatchers("/ai/results/**").authenticated()
                        .anyRequest().authenticated()
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 인증/인가 실패 시 API 설계서 에러 포맷으로 JSON 응답
                .exceptionHandling(exception -> exception
                        // 401 — 인증 실패 (토큰 없음/만료)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    new ObjectMapper().writeValueAsString(
                                            Map.of(
                                                    "success", false,
                                                    "message", "인증이 필요합니다.",
                                                    "code", "UNAUTHORIZED"
                                            )
                                    )
                            );
                        })

                        // 403 — 권한 없음 (타인 리소스 접근)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    new ObjectMapper().writeValueAsString(
                                            Map.of(
                                                    "success", false,
                                                    "message", "접근 권한이 없습니다.",
                                                    "code", "FORBIDDEN"
                                            )
                                    )
                            );
                        })
                );

        return http.build();
    }

    // 비밀번호 암호화 - 회원가입/로그인 시 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 인증 매니저 — 로그인 시 email/password 인증 처리에 사용
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // CORS — 프론트엔드 개발 서버 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",   // React 기본 포트
                "http://localhost:5173"    // Vite 기본 포트
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
