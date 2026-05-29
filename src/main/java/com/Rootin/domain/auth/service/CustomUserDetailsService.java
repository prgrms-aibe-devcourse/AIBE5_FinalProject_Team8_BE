package com.Rootin.domain.auth.service;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 구현체
 *
 * Spring Security가 인증 과정에서 사용자 정보를 로드할 때 이 클래스를 호출한다.
 *
 * 호출되는 시점:
 *   1. JwtAuthenticationFilter에서 토큰 검증 후 email로 사용자 조회 시
 *   2. AuthenticationManager.authenticate() 호출 시 (이메일 로그인)
 *
 * User 엔티티가 UserDetails를 직접 구현하고 있으므로,
 * DB에서 조회한 User 객체를 그대로 반환한다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * email(username)로 사용자 조회
     *
     * @param email 로그인에 사용되는 이메일 (User.getUsername()이 email을 반환하므로)
     * @return UserDetails를 구현한 User 엔티티
     * @throws UsernameNotFoundException 해당 이메일의 사용자가 없을 때
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "해당 이메일의 사용자를 찾을 수 없습니다: " + email
                ));
    }
}
