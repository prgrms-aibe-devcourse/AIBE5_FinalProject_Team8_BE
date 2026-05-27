package com.Rootin.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TODO [로그인 담당자]: 아래 필드/기능 추가 필요
 *  - password, role 등 필드 추가
 *  - UserDetails 구현 (implements UserDetails) → @AuthenticationPrincipal 주입 연동
 *  - 소셜 로그인(Google OAuth2) 처리 로직
 *  - point 적립 로직 (일일 목표 달성 시 포인트 지급 등)
 *
 * ※ AiResult와 연관관계(ManyToOne)로 연결되어 있으므로
 *   id 필드는 반드시 유지해 주세요.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImage;

    /**
     * TODO [포인트 정책]: 적립 로직은 로그인/게임화 담당자가 추가 예정
     * AI 기능 사용 시 차감되는 포인트 잔액
     */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int point;

    /** AI 기능 사용 시 포인트 차감 */
    public void deductPoint(int amount) {
        this.point -= amount;
    }
}
