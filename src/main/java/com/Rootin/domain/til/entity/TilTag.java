package com.Rootin.domain.til.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "til_tag")
@NoArgsConstructor
public class TilTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "til_id", nullable = false)
    private Til til;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static TilTag of(Til til, Tag tag) {
        TilTag tilTag = new TilTag();
        tilTag.til = til;
        tilTag.tag = tag;
        return tilTag;
    }
}
