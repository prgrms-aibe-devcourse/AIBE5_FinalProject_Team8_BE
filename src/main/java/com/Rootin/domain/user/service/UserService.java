package com.Rootin.domain.user.service;

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
        return UserMeResponse.of(user);
    }

    @Transactional
    public UserMeResponse updateUserMe(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        String newNickname = request.getNickname();
        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw CustomException.badRequest("이미 사용 중인 닉네임입니다.");
        }

        user.updateProfile(newNickname, request.getBio());
        return UserMeResponse.of(user);
    }
}
