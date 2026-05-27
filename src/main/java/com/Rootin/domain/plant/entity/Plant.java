package com.Rootin.domain.plant.entity;

import com.Rootin.domain.plant.entity.converter.GrowthStageConverter;
import com.Rootin.domain.plant.entity.enums.Grade;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스에서 제공하는 식물의 마스터 메타데이터를 저장하는 엔티티 클래스입니다.
 * H2/MySQL의 'plant' 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "plant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "grade")
    @Convert(converter = com.Rootin.domain.plant.entity.converter.GradeConverter.class)
    private Grade grade;

    @Column(name = "growth_stage")
    @Convert(converter = GrowthStageConverter.class)
    private GrowthStage growthStage;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "silhouette_url", length = 500)
    private String silhouetteUrl;

    @Builder
    public Plant(String name, Grade grade, GrowthStage growthStage, String imageUrl, String silhouetteUrl) {
        this.name = name;
        this.grade = grade;
        this.growthStage = growthStage;
        this.imageUrl = imageUrl;
        this.silhouetteUrl = silhouetteUrl;
    }
}
