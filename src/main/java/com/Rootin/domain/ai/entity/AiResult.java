package com.Rootin.domain.ai.entity;

import com.Rootin.domain.ai.entity.enums.Difficulty;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_results")
public class AiResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "result_content", nullable = false, columnDefinition = "LONGTEXT")
    private String resultContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false)
    private ToolType toolType;

    // QUIZ일 때만 사용, SUMMARY는 null
    @Column
    private Integer count;

    // QUIZ일 때만 사용, SUMMARY는 null
    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AiResult(Post post, User user, String resultContent,
                    ToolType toolType, Integer count, Difficulty difficulty) {
        this.post = post;
        this.user = user;
        this.resultContent = resultContent;
        this.toolType = toolType;
        this.count = count;
        this.difficulty = difficulty;
    }
}
