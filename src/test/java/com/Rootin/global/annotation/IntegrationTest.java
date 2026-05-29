package com.Rootin.global.annotation;

import com.Rootin.global.config.TestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * 전체 Spring 컨텍스트를 띄우는 통합 테스트용 애노테이션
 * - MySQL DB 사용 (Gradle startTestDb 태스크가 Docker로 자동 프로비저닝)
 * - TestConfig를 통해 OpenAI Mock 자동 주입 → 실제 API 호출 차단
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
