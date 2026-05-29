package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유저 API 컨트롤러
 *
 * 엔드포인트 목록:
 *   GET /api/v1/users/me  로그인한 유저 정보 조회
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =====================================================================
    // 1. 로그인한 유저 정보 조회
    // =====================================================================

    /**
     * GET /api/v1/users/me
     *
     * 인증 필요: ✅
     * 요청: 없음 (JWT에서 사용자 정보 추출)
     * 응답: { id, email, nickname, profileImage, point }
     * 상태코드: 200 OK
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }

        UserMeResponse response = userService.getUserMe(user);
        return ResponseEntity.ok(ApiResponse.success("유저 정보 조회 성공", response));
    }
}
