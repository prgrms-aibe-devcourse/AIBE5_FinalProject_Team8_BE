// [S3 이미지 업로드 기능 추가] PostImage 엔티티
// ERD의 Image 테이블(til_images)에 대응한다.
// TIL 본문에 삽입된 이미지의 S3 URL과 순서를 저장하며, Post(posts 테이블)와 N:1 관계를 맺는다.
package com.Rootin.domain.til.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "til_images")
@NoArgsConstructor
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TIL(포스트) ID: posts 테이블의 id를 외래 키로 참조한다 (ERD: post_id FK)
    // Til 엔티티와 직접 @ManyToOne 대신 post_id 값만 보관하여 불필요한 Til 조회를 방지한다.
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // S3에 저장된 이미지의 접근 가능한 URL (최대 500자, ERD: url VARCHAR(500))
    @Column(nullable = false, length = 500)
    private String url;

    // 이미지 정렬 순서 (클라이언트가 지정한 삽입 순서, ERD: image_order INT)
    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    // 레코드 생성 시각 (자동 기록, ERD: created_at DATETIME)
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 팩토리 메서드: TIL 저장 직후 imageUrl 목록을 순서대로 엔티티로 변환할 때 사용
    public static PostImage of(Long postId, String url, int imageOrder) {
        PostImage img = new PostImage();
        img.postId = postId;
        img.url = url;
        img.imageOrder = imageOrder;
        return img;
    }
}
