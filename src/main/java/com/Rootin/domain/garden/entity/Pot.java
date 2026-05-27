package com.Rootin.domain.garden.entity;

import com.Rootin.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 생성한 '화분(폴더)' 정보를 담는 엔티티 클래스입니다.
 * BaseEntity를 상속받아 생성시간(created_at)을 자동으로 상속받아 로깅합니다.
 * H2/MySQL의 'pot' 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "pot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "total_exp", nullable = false)
    private Integer totalExp;

    @Column(name = "is_displayed")
    private Boolean isDisplayed;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Builder
    public Pot(Long userId, String title, String description, Integer level, Integer totalExp, Boolean isDisplayed, Integer positionX, Integer positionY) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.level = level != null ? level : 1; // 초기 레벨은 기본 1
        this.totalExp = totalExp != null ? totalExp : 0; // 초기 경험치는 기본 0
        this.isDisplayed = isDisplayed != null ? isDisplayed : false;
        this.positionX = positionX;
        this.positionY = positionY;
    }
}
