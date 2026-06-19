// S3 업로드 서비스: Presigned PUT URL 생성, MultipartFile 직접 업로드, 공개 URL 반환 기능을 제공한다
package com.Rootin.global.s3;

import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public String generatePresignedPutUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getS3().getBucket())
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
     * MultipartFile을 S3에 직접 업로드하고 접근 가능한 URL을 반환합니다.
     *
     * @param file      업로드할 파일
     * @param objectKey S3 저장 경로 (예: "til-images/1/2/uuid.jpg")
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadFile(MultipartFile file, String objectKey) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getS3().getBucket())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return getFileUrl(objectKey);
        } catch (IOException e) {
            log.error("S3 업로드 중 파일 스트림 처리 오류. objectKey={}", objectKey, e);
            throw CustomException.internalServerError("파일 스트림 처리 중 오류가 발생했습니다.");
        } catch (S3Exception e) {
            log.error("S3 업로드 실패. objectKey={}, statusCode={}, message={}", objectKey, e.statusCode(), e.getMessage());
            throw CustomException.internalServerError("S3 업로드 중 오류가 발생했습니다.");
        } catch (SdkClientException e) {
            log.error("S3 연결 실패 (네트워크/인증 오류). objectKey={}, message={}", objectKey, e.getMessage());
            throw CustomException.internalServerError("S3 서버와의 연결 중 오류가 발생했습니다.");
        }
    }

    public String getFileUrl(String objectKey) {
        String endpoint = s3Properties.getS3().getEndpoint();
        String bucket = s3Properties.getS3().getBucket();
        String region = s3Properties.getRegion();

        if (endpoint != null && !endpoint.isBlank()) {
            return String.format("%s/%s/%s", endpoint, bucket, objectKey);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);
    }
}