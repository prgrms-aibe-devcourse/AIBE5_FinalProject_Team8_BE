package com.Rootin.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    /**
     * [하위 호환용] 기존 HttpStatus + String 메세지 기반 생성자
     */
    public CustomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }

    /**
     * [ErrorCode 기반] 공통 에러 코드를 사용하는 기본 생성자
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
        this.errorCode = errorCode;
    }

    /**
     * [ErrorCode 기반 + 메시지 커스텀] 공통 에러 코드에 메세지만 동적으로 오버라이드하는 생성자
     */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.status = errorCode.getStatus();
        this.errorCode = errorCode;
    }

    public static CustomException of(ErrorCode errorCode) {
        return new CustomException(errorCode);
    }

    public static CustomException forbidden(String message) {
        return new CustomException(HttpStatus.FORBIDDEN, message);
    }

    public static CustomException notFound(String message) {
        return new CustomException(HttpStatus.NOT_FOUND, message);
    }

    public static CustomException badRequest(String message) {
        return new CustomException(HttpStatus.BAD_REQUEST, message);
    }

    public static CustomException paymentRequired(String message) {
        return new CustomException(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
