package com.Rootin.domain.garden.entity;

import com.Rootin.global.BaseEntity;
import com.Rootin.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.Rootin.domain.garden.constant.PotPolicy.DESCRIPTION_MAX_LENGTH;
import static com.Rootin.domain.garden.constant.PotPolicy.TITLE_MAX_LENGTH;

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

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
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

    /**
     * 화분에 물을 주어 획득한 경험치를 누적하고, 계산된 새 레벨로 업데이트합니다.
     *
     * 서비스에서 필드를 직접 set하지 않고 엔티티 메서드로 변경하는 이유:
     * - "화분 경험치 변경"이라는 도메인 규칙을 Pot 내부에 모을 수 있습니다.
     * - 추후 gainedExp 음수 방지, 최대 레벨 제한 같은 규칙이 생기면 이 메서드 한 곳에 추가하면 됩니다.
     *
     * @param gainedExp 획득한 경험치 양
     * @param nextLevel 계산된 최신 레벨 수치
     */
    public void updateExperienceAndLevel(int gainedExp, int nextLevel) {
        this.totalExp += gainedExp;
        this.level = nextLevel;
    }

    public void updateInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateLayout(Boolean isDisplayed, Integer positionX, Integer positionY) {
        this.isDisplayed = isDisplayed != null ? isDisplayed : false;
        if (Boolean.TRUE.equals(this.isDisplayed)) {
            if (positionX == null || positionX < 0 || positionY == null || positionY < 0) {
                throw CustomException.badRequest("정원에 배치할 경우 좌표(positionX, positionY)는 0 이상이어야 합니다.");
            }
            this.positionX = positionX;
            this.positionY = positionY;
        } else {
            this.positionX = null;
            this.positionY = null;
        }
    }
}
