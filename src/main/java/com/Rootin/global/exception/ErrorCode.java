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

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Auth
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 재발급 후 다시 시도해 주세요."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    EMAIL_PROVIDER_MISMATCH(HttpStatus.CONFLICT, "이미 다른 방식으로 가입된 이메일입니다."),

    // TIL
    TIL_NOT_FOUND(HttpStatus.NOT_FOUND, "TIL을 찾을 수 없습니다."),
    TIL_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 TIL에 대한 권한이 없습니다."),

    // Garden
    POT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 화분입니다."),
    POT_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 화분에 접근할 권한이 없습니다."),
    PLANT_NOT_FOUND(HttpStatus.NOT_FOUND, "식물 정보를 찾을 수 없습니다."),
    ALREADY_WATERED_TODAY(HttpStatus.CONFLICT, "오늘 이미 물주기가 완료된 TIL입니다."),
    NO_ACTIVE_PLANT(HttpStatus.NOT_FOUND, "화분에 심어진 식물이 없습니다."),

    // AI
    AI_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 결과를 찾을 수 없습니다."),
    AI_RESULT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 AI 결과만 삭제할 수 있습니다."),
    AI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 요청 처리 중 오류가 발생했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // Collection
    COLLECTION_EMPTY(HttpStatus.NOT_FOUND, "보유한 식물 컬렉션이 없습니다."),

    // Point
    /** TODO: 포인트 차감 비용 정책 변경 시 AiService.SUMMARY_POINT_COST 값도 함께 수정이 필요합니다 */
    INSUFFICIENT_POINT(HttpStatus.PAYMENT_REQUIRED, "포인트가 부족합니다."),

    // Template
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다."),
    TEMPLATE_DEFAULT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "기본 템플릿은 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
