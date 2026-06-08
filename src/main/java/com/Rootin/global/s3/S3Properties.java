package com.Rootin.global.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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