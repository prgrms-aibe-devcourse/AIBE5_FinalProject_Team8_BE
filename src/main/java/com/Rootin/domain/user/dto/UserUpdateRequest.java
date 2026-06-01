package com.Rootin.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 프로필 수정 요청 DTO
// API: [PATCH] /api/v1/users/me
@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    private String nickname;

    @Size(max = 255, message = "소개는 255자 이하로 입력해주세요.")
    private String bio;
}
