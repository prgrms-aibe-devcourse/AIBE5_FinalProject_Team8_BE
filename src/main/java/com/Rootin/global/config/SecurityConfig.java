package com.Rootin.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO [로그인 담당자]: JWT 필터 추가 예정
 * - JwtAuthenticationFilter를 addFilterBefore()로 등록
 * - 공개 경로(회원가입, 로그인 등) permitAll() 추가
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/test", "/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/tils/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/til-templates/**").permitAll()
                // FIXME [보안 경고]: 현재 JWT 로그인 개발 단계 이전이므로, 화분 API(/api/pots/**)를 임시로 전체 허용(permitAll)해 두었습니다.
                // 이는 테스트를 위한 일시적인 조치이며, 프로덕션 배포 전 반드시 적절한 인증 필터와 함께 인증된 권한(authenticated)을 요구하도록 정책을 전환해야 합니다.
                .requestMatchers("/api/pots/**").permitAll()
                .requestMatchers("/ai/results/**").authenticated()
                .anyRequest().authenticated()

            );

        return http.build();
    }
}
