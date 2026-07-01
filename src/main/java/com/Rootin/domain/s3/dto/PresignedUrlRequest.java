// [S3 이미지 업로드 기능 추가] PresignedUrlRequest
// tilId 필드 추가: 기존 TIL 수정 시 tilId를 전달하면 S3 경로에 TIL ID가 포함된다.
// 신규 TIL 작성 시(tilId 미전달)에는 경로에 0이 사용된다.
package com.Rootin.domain.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
public class PresignedUrlRequest {
    @NotBlank(message = "contentType은 필수입니다.")
    private String contentType;

    @NotNull(message = "화분 ID는 필수입니다.")
    private Long potId;

    private String fileName;

    // [S3 이미지 업로드 기능 추가] TIL 수정 시 TIL ID 전달 (신규 TIL 작성 시 null)
    // null이면 S3 경로의 tilId 자리에 0을 사용한다.
    private Long tilId;
}
