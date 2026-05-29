package com.Rootin.domain.user.service;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * 인증된 유저의 기본 정보를 반환한다.
     *
     * JwtAuthenticationFilter에서 SecurityContext에 적재된 User 엔티티를 그대로 사용하므로
     * 추가 DB 조회 없이 DTO로 변환한다.
     *
     * @param user @AuthenticationPrincipal로 주입된 User 엔티티
     * @return UserMeResponse
     */
    public UserMeResponse getUserMe(User user) {
        return UserMeResponse.of(user);
    }
}
