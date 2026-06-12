package com.Rootin.global.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// ───────── Application의 Cloud.aws 설정값 ───────────────────────────
// 설정(리전, 버킷, 키, 엔드포인트)을 담는 그릇
// 비어있는 칸은 S3 Config에서 IAM Role을 사용

@ConfigurationProperties(prefix = "cloud.aws")
@Getter
@Setter
public class S3Properties {

    private String region = "ap-northeast-2";
    private Credentials credentials = new Credentials();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Credentials {
        private String accessKey = "";
        private String secretKey = "";
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String endpoint = "";
    }
}