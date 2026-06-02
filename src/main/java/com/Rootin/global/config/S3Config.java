package com.Rootin.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${cloud.aws.region:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.credentials.access-key:test}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:test}")
    private String secretKey;

    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Presigner s3Presigner() {
        String resolvedRegion = (region == null || region.isBlank()) ? "ap-northeast-2" : region;
        String resolvedAccessKey = (accessKey == null || accessKey.isBlank()) ? "test" : accessKey;
        String resolvedSecretKey = (secretKey == null || secretKey.isBlank()) ? "test" : secretKey;

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(resolvedRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(resolvedAccessKey, resolvedSecretKey)
                        )
                );

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            );
        }

        return builder.build();
    }
}
