package com.Rootin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class RootinApplication {
    public static void main(String[] args) {
        // JVM 타임존을 서비스 기준(KST)으로 고정합니다.
        // Docker/CI 환경의 기본 타임존(UTC)과 관계없이 LocalDate.now() 등이 일관된 KST 기준으로 동작합니다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(RootinApplication.class, args);
    }
}
