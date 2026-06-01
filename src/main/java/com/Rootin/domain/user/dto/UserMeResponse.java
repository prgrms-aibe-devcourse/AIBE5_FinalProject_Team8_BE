package com.Rootin.domain.user.dto;

import com.Rootin.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

// 로그인한 유저 정보 응답 DTO
// API: [GET] /api/v1/users/me, [PATCH] /api/v1/users/me
@Getter
@Builder
public class UserMeResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private String bio;
    private int point;

    public static UserMeResponse of(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .point(user.getPoint())
                .build();
    }
}
