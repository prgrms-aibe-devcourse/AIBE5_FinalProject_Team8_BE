package com.Rootin.global.config;

import com.Rootin.global.filter.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Import(RateLimitConfig.class)
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Primary
    public RateLimitFilter rateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitConfig, objectMapper) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return true;
            }
        };
    }
}