package com.Rootin.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /**
     * Docker가 Testcontainers Java 클라이언트로 정상 감지되는 환경(Linux/CI)에서만 빈 등록.
     *
     * Windows Docker Desktop WSL2 환경에서는 named pipe가 빈 응답을 반환해
     * isDockerAvailable()=false → 빈 미생성 → application-test.yml(startTestDb MySQL) 사용.
     *
     * Linux/CI에서는 isDockerAvailable()=true → 빈 생성 → @ServiceConnection이
     * datasource URL을 컨테이너 포트로 자동 오버라이드.
     */
    @Bean
    @ServiceConnection
    @ConditionalOnExpression(
            "#{T(org.testcontainers.DockerClientFactory).instance().isDockerAvailable()}"
    )
    MySQLContainer<?> mySQLContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("rootin_test")
                .withUsername("root")
                .withPassword("")
                // mysqladmin ping 성공 시점이 아닌 실제 포트 오픈까지 대기
                // CI 환경에서 이미지 풀 시간 포함 최대 3분 허용
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(3));
    }
}