package com.Rootin.domain.user.dto;

import com.Rootin.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 로그인한 유저 정보 응답 DTO
// API: [GET] /api/v1/users/me, [PATCH] /api/v1/users/me
@Getter
@Builder
public class UserMeResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String bio;
    private int point;
    private String provider;
    private long tilCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static UserMeResponse of(User user, long tilCount) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .bio(user.getBio())
                .point(user.getPoint())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .tilCount(tilCount)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
