// TIL 엔티티: S3에 업로드된 썸네일 이미지 URL(thumbnailUrl)을 저장하는 컬럼과 팩토리 메서드를 포함한다
package com.Rootin.domain.til.entity;

import com.Rootin.domain.garden.entity.Pot;
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

    /** TIL 작성 시 업로드된 썸네일 이미지의 S3 URL */
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @OneToMany(mappedBy = "til", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TilTag> tilTags = new ArrayList<>();

    protected Til(User user, String title, String content, Pot pot, String thumbnailUrl) {
        super(user, title, content, PostStatus.PUBLISHED);
        this.pot = pot;
        this.publishedAt = LocalDateTime.now();
        this.thumbnailUrl = thumbnailUrl;
    }

    protected Til(User user, String title, String content, Pot pot, PostStatus status, String thumbnailUrl) {
        super(user, title, content, status);
        this.pot = pot;
        this.thumbnailUrl = thumbnailUrl;
    }

    /** 썸네일 없이 TIL 생성 (하위 호환용) */
    public static Til create(User user, String title, String content, Pot pot) {
        return new Til(user, title, content, pot, null);
    }

    /** 썸네일 포함 TIL 생성 */
    public static Til create(User user, String title, String content, Pot pot, String thumbnailUrl) {
        return new Til(user, title, content, pot, thumbnailUrl);
    }

    /** 썸네일 없이 임시저장 생성 (하위 호환용) */
    public static Til createDraft(User user, String title, String content, Pot pot) {
        return new Til(user, title, content, pot, PostStatus.DRAFT, null);
    }

    /** 썸네일 포함 임시저장 생성 */
    public static Til createDraft(User user, String title, String content, Pot pot, String thumbnailUrl) {
        return new Til(user, title, content, pot, PostStatus.DRAFT, thumbnailUrl);
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

}
