package com.Rootin.global.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 클레임에서 생성하는 경량 인증 주체(Principal).
 *
 * JwtAuthenticationFilter가 Access Token의 userId·email·role 클레임만으로
 * Authentication 객체를 구성할 때 사용한다.
 * DB 조회 없이 생성되므로, 매 요청마다 발생하던 불필요한 SELECT를 제거한다.
 *
 * 컨트롤러에서 주입 방법:
 * {@code @AuthenticationPrincipal JwtUserDetails userDetails}
 * → userDetails.getUserId() 로 userId 획득
 */
public class JwtUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUserDetails(Long userId, String email,
                          Collection<? extends GrantedAuthority> authorities) {
        this.userId    = userId;
        this.email     = email;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    // ── UserDetails 구현 ──────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return null; // JWT 기반 인증에서는 비밀번호 불필요
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
