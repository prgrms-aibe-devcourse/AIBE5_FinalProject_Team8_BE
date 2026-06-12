package com.Rootin.domain.til.entity;

import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TODO [로그인 담당자]: User 엔티티 완성 후 연관관계 확인 필요
 *
 * ※ Til(자식)이 AiResult와 @ManyToMany로 연결되어 있으므로
 *   id, user 필드는 반드시 유지해 주세요.
 *   (ai_result_til 중간 테이블의 post_id = til.post_id = posts.id)
 */
@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "posts")
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String title;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected Post(User user, String title, String content, PostStatus status) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void publish() {
        this.status = PostStatus.PUBLISHED;
    }
}
