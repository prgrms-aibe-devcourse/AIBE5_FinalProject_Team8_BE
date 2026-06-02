package com.Rootin.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String code; // 에러 코드 (성공 시 null → JSON에서 제외)

    // =====================================================================
    // [성공 응답 메시지 빌더 - 팀원 공유용]
    // 기존에 ApiResponse.ok()와 ApiResponse.success() 두 벌의 이름으로 동일 동작이
    // 중복 제공되어 혼재되던 문제를 해결하고자 ok()를 지우고 success() 하나로 일관성을 확보했습니다.
    // =====================================================================
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    // 에러 메시지
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public static ApiResponse<Void> error(String message, String code) {
        return new ApiResponse<>(false, message, null, code);
    }
}
