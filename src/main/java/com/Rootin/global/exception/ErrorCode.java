package com.Rootin.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // TIL
    TIL_NOT_FOUND(HttpStatus.NOT_FOUND, "TIL을 찾을 수 없습니다."),
    TIL_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 TIL에 대한 권한이 없습니다."),

    // Template
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다."),
    TEMPLATE_DEFAULT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "기본 템플릿은 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
