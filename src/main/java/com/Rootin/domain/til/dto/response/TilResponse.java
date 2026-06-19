// TIL 응답 DTO: TIL 조회·작성 시 클라이언트로 반환하는 데이터 구조 (thumbnailUrl 필드 추가)
package com.Rootin.domain.til.dto.response;

import com.Rootin.domain.til.entity.Til;

import java.time.LocalDateTime;
import java.util.List;

public record TilResponse(
        Long tilId,
        String title,
        String content,
        String thumbnailUrl,
        List<String> tags,
        AuthorInfo author,
        Long potId,
        String potName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
    public record AuthorInfo(String nickname, String profileImageUrl) {}

    public static TilResponse from(Til til) {
        List<String> tags = til.getTilTags().stream()
                .map(tt -> tt.getTag().getName())
                .toList();

        AuthorInfo author = new AuthorInfo(
                til.getUser().getNickname(),
                til.getUser().getProfileImage()
        );

        return new TilResponse(
                til.getId(),
                til.getTitle(),
                til.getContent(),
                til.getThumbnailUrl(),
                tags,
                author,
                til.getPot().getId(),
                til.getPot().getTitle(),
                til.getCreatedAt(),
                til.getUpdatedAt(),
                til.getPublishedAt()
        );
    }
}
