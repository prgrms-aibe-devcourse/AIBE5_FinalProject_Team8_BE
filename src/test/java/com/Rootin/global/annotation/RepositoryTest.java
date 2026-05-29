package com.Rootin.global.annotation;

import com.Rootin.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * 실제 DB 설정을 유지하는 Repository 테스트용 애노테이션
 * - JPA 관련 빈만 로드 (@DataJpaTest)
 * - application-test.yml의 H2 설정 사용 (DB 교체 없음)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@Import(JpaAuditingConfig.class)
public @interface RepositoryTest {
}
