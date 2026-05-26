package com.Rootin.annotation;

import com.Rootin.config.TestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * 전체 Spring 컨텍스트를 띄우는 통합 테스트용 애노테이션
 * - H2 인메모리 DB 사용 (test 프로파일)
 * - TestConfig를 통해 OpenAI Mock 자동 주입 → 실제 API 호출 차단
 * 사용 예: Service + Repository 흐름 전체 검증
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@ExtendWith(SpringExtension.class)
public @interface IntegrationTest {
}
