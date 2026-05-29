package com.Rootin.global.annotation;

import com.Rootin.global.config.TestConfig;
import com.Rootin.global.config.TestcontainersConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * 전체 Spring 컨텍스트를 띄우는 통합 테스트용 애노테이션
 * - MySQL DB 사용 (Testcontainers가 Docker 컨테이너를 자동 기동·종료)
 * - Linux/CI: Testcontainers가 MySQL을 직접 관리
 * - Windows: build.gradle의 startTestDb 태스크가 Docker CLI로 MySQL 기동
 * - TestConfig를 통해 OpenAI Mock 자동 주입 → 실제 API 호출 차단
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import({TestConfig.class, TestcontainersConfig.class})
@ExtendWith(SpringExtension.class)
public @interface IntegrationTest {
}
