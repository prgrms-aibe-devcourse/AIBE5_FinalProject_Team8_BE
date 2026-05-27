package com.Rootin.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/test", "/h2-console/**").permitAll()
                .requestMatchers("/ai/results/**").authenticated()
                .anyRequest().authenticated()

            );

        return http.build();
    }
}
