package com.Rootin.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 기능을 활성화하기 위한 별도의 설정 클래스입니다.
 * main 애플리케이션 클래스에서 @EnableJpaAuditing을 분리함으로써,
 * JPA 메타모델이 존재하지 않는 @WebMvcTest 등 슬라이스 테스트 구동 시
 * 컨텍스트 로딩이 실패하는 문제를 방지합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
