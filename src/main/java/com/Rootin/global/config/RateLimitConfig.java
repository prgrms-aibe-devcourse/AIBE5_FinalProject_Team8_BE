package com.Rootin.global.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // AI 엔드포인트 사용자별 분당 최대 요청 수
    public static final int AI_REQUESTS_PER_MINUTE = 5;

    public Bucket newAiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(AI_REQUESTS_PER_MINUTE)
                .refillGreedy(AI_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
