package com.Rootin.domain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ai_result_til 복합 PK (result_id, post_id)
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AiResultTilId implements Serializable {

    @Column(name = "AIResult_id")
    private Long resultId;

    @Column(name = "post_id")
    private Long postId;
}
