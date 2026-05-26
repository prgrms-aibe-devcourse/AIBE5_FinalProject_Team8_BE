package com.Rootin.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * Controller 레이어 단위 테스트용 애노테이션
 * - MockMvc 자동 구성 (@WebMvcTest)
 * - Service 등 의존성은 @MockBean으로 주입
 * 사용 예: 요청/응답 형식, 유효성 검사, HTTP 상태코드 검증
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public @interface ControllerTest {
}
