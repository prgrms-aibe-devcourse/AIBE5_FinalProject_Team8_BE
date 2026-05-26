package com.Rootin.domain.til.entity;

import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * TODO [TIL 담당자]: 아래 필드 추가 필요
 *  - title, content, tags, isPublic, createdAt 등 필드 추가
 *  - 시리즈(화분) 연관관계 추가
 *
 * ※ AiResult와 연관관계(ManyToOne)로 연결되어 있으므로
 *   id, user 필드는 반드시 유지해 주세요.
 *   (AiResultService에서 post.getUser().getId()로 소유자 검증 중)
 */
@Getter
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
