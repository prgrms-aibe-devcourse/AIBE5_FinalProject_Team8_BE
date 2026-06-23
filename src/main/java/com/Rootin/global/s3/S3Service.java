// S3 업로드 서비스: Presigned PUT URL 생성, MultipartFile 직접 업로드, 공개 URL 반환 기능을 제공한다
// [S3 이미지 업로드 기능 추가] deleteFile(), extractObjectKey() 메서드 추가
package com.Rootin.global.s3;

import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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

    /**
     * [S3 이미지 업로드 기능 추가] S3 오브젝트 키로 파일을 삭제합니다.
     * TIL 이미지 삭제 또는 TIL 삭제 시 연관 S3 파일을 정리하는 데 사용합니다.
     * 삭제 실패 시 예외를 던지지 않고 에러 로그만 남겨 TIL 트랜잭션에 영향을 주지 않습니다.
     *
     * @param objectKey S3 오브젝트 키 (예: "til-images/uuid/potId/tilId/filename.jpg")
     */
    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getS3().getBucket())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일 삭제 완료. objectKey={}", objectKey);
        } catch (S3Exception e) {
            log.error("S3 파일 삭제 실패. objectKey={}, statusCode={}, message={}",
                    objectKey, e.statusCode(), e.getMessage());
        } catch (SdkClientException e) {
            log.error("S3 파일 삭제 중 연결 오류. objectKey={}, message={}", objectKey, e.getMessage());
        }
    }

    /**
     * [S3 이미지 업로드 기능 추가] S3 파일 URL에서 오브젝트 키를 추출합니다.
     * 이미지 삭제 시 DB에 저장된 URL로부터 S3 키를 역산할 때 사용합니다.
     *
     * 지원 URL 형식:
     *   - 표준 AWS: https://{bucket}.s3.{region}.amazonaws.com/{key}
     *   - 커스텀 엔드포인트: {endpoint}/{bucket}/{key}
     *
     * @param url S3 이미지 공개 URL
     * @return S3 오브젝트 키, 파싱 실패 시 null 반환 (로그만 기록)
     */
    public String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String bucket = s3Properties.getS3().getBucket();
            String endpoint = s3Properties.getS3().getEndpoint();

            if (endpoint != null && !endpoint.isBlank()) {
                // 커스텀 엔드포인트 형식: {endpoint}/{bucket}/{key}
                String prefix = endpoint + "/" + bucket + "/";
                if (url.startsWith(prefix)) {
                    return url.substring(prefix.length());
                }
            } else {
                // 표준 AWS 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
                String prefix = "https://" + bucket + ".s3.";
                if (url.startsWith(prefix)) {
                    // amazonaws.com/ 이후 부분이 key
                    int keyStart = url.indexOf(".amazonaws.com/");
                    if (keyStart >= 0) {
                        return url.substring(keyStart + ".amazonaws.com/".length());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("S3 URL에서 오브젝트 키 추출 실패. url={}", url, e);
        }
        log.warn("S3 URL 형식이 예상과 다릅니다. 삭제를 건너뜁니다. url={}", url);
        return null;
    }

    /**
     * [S3 이미지 업로드 기능 추가] S3 파일 URL로 파일을 직접 삭제합니다.
     * URL에서 오브젝트 키를 추출한 후 deleteFile()을 호출합니다.
     *
     * @param url S3 이미지 공개 URL
     */
    public void deleteFileByUrl(String url) {
        String objectKey = extractObjectKey(url);
        if (objectKey != null) {
            deleteFile(objectKey);
        }
    }
}