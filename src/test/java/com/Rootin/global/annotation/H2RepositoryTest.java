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
 * H2 인메모리 DB를 강제 적용하는 Repository 테스트용 애노테이션
 * - JPA 관련 빈만 로드 (@DataJpaTest)
 * - MySQL 설정을 무시하고 H2로 대체
 */
// CRUD 기능 정도만을 빠른 속도로 검사하는 H2 RepositoryTest
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@Import(JpaAuditingConfig.class)
public @interface H2RepositoryTest {
}
