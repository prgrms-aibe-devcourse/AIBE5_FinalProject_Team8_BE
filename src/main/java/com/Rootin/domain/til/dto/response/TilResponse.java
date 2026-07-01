// TIL 응답 DTO: TIL 조회·작성 시 클라이언트로 반환하는 데이터 구조
// [S3 이미지 업로드 기능 추가] images 필드 추가: TIL 본문에 삽입된 이미지 목록을 반환한다.
// 클라이언트는 이 목록을 통해 수정 진입 시 기존 이미지를 복원하고, 삭제할 이미지 ID를 식별한다.
package com.Rootin.domain.til.dto.response;

import com.Rootin.domain.til.entity.PostImage;
import com.Rootin.domain.til.entity.Til;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record TilResponse(
        Long tilId,
        String title,
        String content,
        String thumbnailUrl,
        List<String> tags,
        // [S3 이미지 업로드 기능 추가] 본문 삽입 이미지 목록 (id·url·imageOrder 포함)
        List<PostImageResponse> images,
        AuthorInfo author,
        Long potId,
        String potName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
    public record AuthorInfo(String nickname, String profileImageUrl) {}

    // 이미지 목록 없이 TIL만으로 변환 (빈 이미지 목록 반환)
    // 기존 from(Til) 호출부와의 하위 호환성을 유지한다.
    public static TilResponse from(Til til) {
        return from(til, Collections.emptyList());
    }

    // [S3 이미지 업로드 기능 추가] 이미지 목록을 포함한 완전한 TilResponse 생성
    // TilService에서 DB 조회한 PostImage 목록을 함께 전달할 때 사용한다.
    public static TilResponse from(Til til, List<PostImage> images) {
        List<String> tags = til.getTilTags().stream()
                .map(tt -> tt.getTag().getName())
                .toList();

        AuthorInfo author = new AuthorInfo(
                til.getUser().getNickname(),
                til.getUser().getProfileImage()
        );

        List<PostImageResponse> imageResponses = images.stream()
                .map(PostImageResponse::from)
                .toList();

        return new TilResponse(
                til.getId(),
                til.getTitle(),
                til.getContent(),
                til.getThumbnailUrl(),
                tags,
                imageResponses,
                author,
                til.getPot().getId(),
                til.getPot().getTitle(),
                til.getCreatedAt(),
                til.getUpdatedAt(),
                til.getPublishedAt()
        );
    }
}
