// [S3 이미지 업로드 기능 추가] PostImageResponse DTO
// TIL 응답에 포함될 이미지 정보 (id, url, imageOrder).
// TIL 수정 진입 시 클라이언트가 기존 이미지를 복원하는 데 사용한다.
package com.Rootin.domain.til.dto.response;

import com.Rootin.domain.til.entity.PostImage;

public record PostImageResponse(
        Long id,          // 이미지 레코드 ID (삭제 요청 시 deletedImageIds에 사용)
        String url,       // S3 이미지 URL
        int imageOrder    // 삽입 순서
) {
    public static PostImageResponse from(PostImage image) {
        return new PostImageResponse(image.getId(), image.getUrl(), image.getImageOrder());
    }
}
