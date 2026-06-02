package com.Rootin.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    /**
     * S3 PUT Presigned URL을 생성한다.
     *
     * @param objectKey   S3 오브젝트 키 (예: profile-images/1/uuid.jpg)
     * @param contentType 업로드할 파일의 Content-Type (예: image/jpeg)
     * @return presigned PUT URL (유효시간 10분)
     */
    public String generatePresignedPutUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * objectKey로 S3 공개 URL을 조합해 반환한다.
     *
     * @param objectKey S3 오브젝트 키
     * @return https://{bucket}.s3.{region}.amazonaws.com/{objectKey}
     */
    public String getFileUrl(String objectKey) {
        if (endpoint != null && !endpoint.isBlank()) {
            return String.format("%s/%s/%s", endpoint, bucket, objectKey);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);
    }
}
