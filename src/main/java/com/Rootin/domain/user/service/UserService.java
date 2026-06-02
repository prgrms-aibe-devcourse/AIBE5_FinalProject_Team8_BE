package com.Rootin.domain.user.service;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.dto.UserMeResponse;
import com.Rootin.domain.user.dto.UserUpdateRequest;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TilRepository tilRepository;

    /**
     * 인증된 유저의 기본 정보를 반환한다.
     *
     * JwtAuthenticationFilter가 DB 조회 없이 JwtUserDetails만 설정하므로,
     * 최신 정보를 보장하기 위해 userId로 DB에서 직접 조회한다.
     *
     * @param userId JWT 클레임에서 추출한 사용자 ID
     * @return UserMeResponse
     */
    @Transactional(readOnly = true)
    public UserMeResponse getUserMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        long tilCount = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);
        return UserMeResponse.of(user, tilCount);
    }

    /**
     * 회원 탈퇴 (소프트 딜리트)
     *
     * 처리 흐름:
     *   1. userId로 유저 조회
     *   2. deactivate() 호출 — isDeleted=true, deletedAt=now() 기록
     *   3. Refresh Token 전체 삭제 — 이후 토큰 갱신 불가
     *
     * 30일 후 스케줄러에 의해 영구 삭제된다.
     *
     * @param userId JWT 클레임에서 추출한 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        user.deactivate();
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public UserMeResponse updateUserMe(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        String newNickname = request.getNickname();
        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw CustomException.badRequest("이미 사용 중인 닉네임입니다.");
        }

        user.updateProfile(newNickname, request.getBio(), request.getProfileImageUrl());
        long tilCount = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);
        return UserMeResponse.of(user, tilCount);
    }
}
