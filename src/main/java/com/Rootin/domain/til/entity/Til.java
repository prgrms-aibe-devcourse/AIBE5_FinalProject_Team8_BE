package com.Rootin.domain.til.entity;

import com.Rootin.domain.pot.entity.Pot;
import com.Rootin.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "til")
@PrimaryKeyJoinColumn(name = "post_id")
@NoArgsConstructor
public class Til extends Post {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pot_id", nullable = false)
    private Pot pot;

    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "til", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TilTag> tilTags = new ArrayList<>();

    protected Til(User user, String title, String content, Pot pot) {
        super(user, title, content, PostStatus.PUBLISHED);
        this.pot = pot;
        this.publishedAt = LocalDateTime.now();
    }

    public static Til create(User user, String title, String content, Pot pot) {
        return new Til(user, title, content, pot);
    }

}
