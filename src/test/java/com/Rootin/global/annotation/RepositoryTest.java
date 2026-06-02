package com.Rootin.global.annotation;

import com.Rootin.global.config.TestcontainersConfig;
import com.Rootin.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * MySQL DB를 사용하는 Repository 테스트용 애노테이션
 * - JPA 관련 빈만 로드 (@DataJpaTest)
 * - Testcontainers가 Docker MySQL을 자동 기동·종료 (@ServiceConnection)
 * - Linux/CI: Testcontainers 정상 동작
 * - Windows: build.gradle의 startTestDb 태스크가 Docker CLI로 MySQL 기동
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
@ExtendWith(SpringExtension.class)
public @interface RepositoryTest {
}
