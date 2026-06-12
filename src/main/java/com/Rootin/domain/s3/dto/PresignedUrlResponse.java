package com.Rootin.domain.s3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String imageUrl;        // 업로드 후 TIL에 넣을 URL
    private String presignedUrl;    //
}
