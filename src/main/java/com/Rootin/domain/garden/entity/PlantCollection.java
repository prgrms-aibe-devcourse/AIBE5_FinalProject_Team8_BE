package com.Rootin.domain.garden.entity;

import com.Rootin.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plant_collection",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "plant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlantCollection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plant_id", nullable = false)
    private Long plantId;

    @Builder
    public PlantCollection(Long userId, Long plantId) {
        this.userId = userId;
        this.plantId = plantId;
    }
}
