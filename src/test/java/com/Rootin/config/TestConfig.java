package com.Rootin.config;

import com.openai.client.OpenAIClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트 전용 설정 - test 프로파일에서 자동 적용
 * OpenAIClient를 Mock으로 교체하여 실제 API 호출 차단
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public OpenAIClient openAIClient() {
        return mock(OpenAIClient.class);
    }
}
