package com.Rootin.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트 전용 Security 설정
 *
 * 테스트 환경에서는 인증/인가를 검사하지 않도록 모든 요청을 permitAll 처리한다.
 * @WebMvcTest 사용 시 @Import(TestSecurityConfig.class)로 가져와서 사용한다.
 *
 * 사용 예시:
 *   @WebMvcTest(controllers = SomeController.class)
 *   @Import(TestSecurityConfig.class)
 *   class SomeControllerTest { ... }
 */
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    @Order(0) // 실제 SecurityConfig보다 우선 적용
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}