package com.Rootin.domain.user.controller;

import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.service.UserService;
import com.Rootin.global.common.ApiResponse;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        UserMeResponse response = userService.getUserMe(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("유저 정보 조회 성공", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        userService.deleteUser(userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> updateMe(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        if (userDetails == null) {
            throw CustomException.badRequest("로그인한 사용자 정보가 없습니다.");
        }
        UserMeResponse response = userService.updateUserMe(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 성공", response));
    }
}
