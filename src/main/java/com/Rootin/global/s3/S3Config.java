// S3 연결 설정: Presigned URL 발급용 S3Presigner와 서버 직접 업로드용 S3Client 빈을 등록한다
package com.Rootin.global.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        String region = props.getRegion();
        String accessKey = props.getCredentials().getAccessKey();
        String secretKey = props.getCredentials().getSecretKey();
        String endpoint = props.getS3().getEndpoint();

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region));

        // 명시적 자격증명이 있으면 사용, 없으면 EC2 IAM 역할(DefaultCredentialsProvider) 사용
        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client(S3Properties props) {
        String region = props.getRegion();
        String accessKey = props.getCredentials().getAccessKey();
        String secretKey = props.getCredentials().getSecretKey();
        String endpoint = props.getS3().getEndpoint();

        AwsCredentialsProvider credentialsProvider =
                (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank())
                        ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                        : DefaultCredentialsProvider.create();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());
        }

        return builder.build();
    }
}