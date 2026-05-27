package com.Rootin.domain.ai.entity;

import com.Rootin.domain.til.entity.Til;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ai_result_til 중간 테이블 엔티티
 * AiResult ↔ Til 다대다 관계를 명시적으로 관리
 * user_id 컬럼 포함 (ERD 기준)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_result_til")
public class AiResultTil {

    @EmbeddedId
    private AiResultTilId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("resultId")
    @JoinColumn(name = "AIResult_id")
    private AiResult aiResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Til til;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public AiResultTil(AiResult aiResult, Til til, Long userId) {
        this.id = new AiResultTilId(aiResult.getId(), til.getId());
        this.aiResult = aiResult;
        this.til = til;
        this.userId = userId;
    }
}
