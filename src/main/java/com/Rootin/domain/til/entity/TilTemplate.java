package com.Rootin.domain.til.entity;

import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "til_templates")
@NoArgsConstructor
public class TilTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private boolean isDefault;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private TilTemplate(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.isDefault = false;
    }

    public static TilTemplate create(User user, String title, String content) {
        return new TilTemplate(user, title, content);
    }
}
