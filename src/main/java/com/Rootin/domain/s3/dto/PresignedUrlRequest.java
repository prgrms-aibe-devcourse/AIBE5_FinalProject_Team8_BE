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
}
