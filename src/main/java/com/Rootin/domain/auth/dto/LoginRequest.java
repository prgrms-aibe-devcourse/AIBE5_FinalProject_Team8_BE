package com.Rootin.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이메일 로그인 요청 DTO
// API: [POST] /auth/login
@Getter
@NoArgsConstructor
public class LoginRequest {
    // 이메일 — 등록된 이메일
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    // 비밀번호 — 등록된 비밀번호
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
