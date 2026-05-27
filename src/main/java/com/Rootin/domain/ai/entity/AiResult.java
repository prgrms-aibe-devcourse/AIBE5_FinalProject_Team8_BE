package com.Rootin.domain.ai.entity;

import com.Rootin.domain.ai.entity.enums.Difficulty;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_results")
public class AiResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // QUIZ일 때만 사용, SUMMARY는 null (추후 난이도 설정 기능에 사용)
    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 이 AI 결과 생성에 사용된 TIL 목록
     * ai_result_til 중간 테이블 엔티티로 관리 (user_id 포함)
     */
    @OneToMany(mappedBy = "aiResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiResultTil> aiResultTils = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AiResult(User user, String resultContent,
                    ToolType toolType, Integer count, Difficulty difficulty) {
        this.user = user;
        this.resultContent = resultContent;
        this.toolType = toolType;
        this.count = count;
        this.difficulty = difficulty;
    }

    /** 저장 후 TIL 연결 시 사용 */
    public void addTil(Til til) {
        AiResultTil aiResultTil = AiResultTil.builder()
                .aiResult(this)
                .til(til)
                .userId(this.user.getId())
                .build();
        this.aiResultTils.add(aiResultTil);
    }

    /** 연결된 TIL 목록 반환 */
    public List<Til> getTils() {
        return aiResultTils.stream()
                .map(AiResultTil::getTil)
                .toList();
    }
}
