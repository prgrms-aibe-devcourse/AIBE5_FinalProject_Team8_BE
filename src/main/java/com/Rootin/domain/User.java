package com.Rootin.domain;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * TODO [로그인 담당자]: 아래 필드/기능 추가 필요
 *  - nickname, password, profileImage, role 등 필드 추가
 *  - UserDetails 구현 (implements UserDetails) → @AuthenticationPrincipal 주입 연동
 *  - 소셜 로그인(Google OAuth2) 처리 로직
 *
 * ※ AiResult와 연관관계(ManyToOne)로 연결되어 있으므로
 *   id 필드는 반드시 유지해 주세요.
 */
@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
}
