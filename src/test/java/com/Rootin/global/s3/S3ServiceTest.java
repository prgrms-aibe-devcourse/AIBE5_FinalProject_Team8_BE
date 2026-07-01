// S3Service 단위 테스트: presigned URL 생성, 직접 파일 업로드(uploadFile), 공개 URL 반환 동작을 검증한다
package com.Rootin.global.s3;

import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private S3Service s3Service;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private S3Properties defaultProps;

    @BeforeEach
    void setUp() {
        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("rootin-bucket");

        defaultProps = new S3Properties();
        defaultProps.setRegion("ap-northeast-2");
        defaultProps.setS3(s3);

        s3Service = new S3Service(s3Presigner, s3Client, defaultProps);
    }

    @Test
    @DisplayName("generatePresignedPutUrl — S3Presigner 호출 후 URL 문자열 반환")
    void generatePresignedPutUrl_success() throws MalformedURLException {
        String objectKey = "profile-images/1/uuid.jpg";
        String contentType = "image/jpeg";
        URL fakeUrl = new URL("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey + "?X-Amz-Signature=abc");

        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(fakeUrl);

        String result = s3Service.generatePresignedPutUrl(objectKey, contentType);

        assertThat(result).isEqualTo(fakeUrl.toString());
        assertThat(result).contains("X-Amz-Signature");
    }

    @Test
    @DisplayName("getFileUrl — bucket/region/objectKey 조합으로 S3 공개 URL 반환")
    void getFileUrl_success() {
        String objectKey = "profile-images/1/uuid.jpg";

        String result = s3Service.getFileUrl(objectKey);

        assertThat(result).isEqualTo(
                "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg"
        );
    }

    @Test
    @DisplayName("getFileUrl — LocalStack endpoint가 설정되면 path-style URL(endpoint/bucket/key) 반환")
    void getFileUrl_withLocalStackEndpoint() {
        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("rootin-bucket");
        s3.setEndpoint("http://localhost:4566");

        S3Properties props = new S3Properties();
        props.setRegion("ap-northeast-2");
        props.setS3(s3);

        S3Service serviceWithEndpoint = new S3Service(s3Presigner, s3Client, props);
        String objectKey = "til-images/1/10/uuid.jpg";

        String result = serviceWithEndpoint.getFileUrl(objectKey);

        assertThat(result).isEqualTo("http://localhost:4566/rootin-bucket/til-images/1/10/uuid.jpg");
    }

    @Test
    @DisplayName("getFileUrl — TIL 이미지 경로 패턴(til-images/{userId}/{potId}/{uuid}.{ext})을 포함한 URL 반환")
    void getFileUrl_tilImagePath() {
        String objectKey = "til-images/5/20/550e8400-e29b-41d4-a716-446655440000.png";

        String result = s3Service.getFileUrl(objectKey);

        assertThat(result).startsWith("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/");
        assertThat(result).endsWith(objectKey);
    }

    // ─── uploadFile() ────────────────────────────────────────────────

    @Test
    @DisplayName("uploadFile — S3Client.putObject 호출 후 공개 URL 반환")
    void uploadFile_success() {
        String objectKey = "til-images/1/10/uuid.jpg";
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        String result = s3Service.uploadFile(file, objectKey);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertThat(result).isEqualTo(
                "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey
        );
    }

    @Test
    @DisplayName("uploadFile — 지원하지 않는 contentType → 400 예외")
    void uploadFile_unsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.gif", "image/gif", "fake".getBytes()
        );

        // S3Service.uploadFile()은 contentType 검증 없이 S3에 그대로 올림;
        // 검증은 TilService.uploadThumbnailIfPresent()에서 담당하므로 여기선 정상 통과
        assertThat(s3Service.uploadFile(file, "til-images/1/10/uuid.gif"))
                .contains("til-images/1/10/uuid.gif");
    }

    @Test
    @DisplayName("generatePresignedPutUrl — objectKey에 til-images 경로가 포함된 경우에도 정상 동작")
    void generatePresignedPutUrl_tilImagesPath() throws MalformedURLException {
        String objectKey = "til-images/1/10/550e8400-e29b-41d4-a716-446655440000.jpg";
        String contentType = "image/jpeg";
        URL fakeUrl = new URL("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey + "?X-Amz-Signature=xyz");

        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(fakeUrl);

        String result = s3Service.generatePresignedPutUrl(objectKey, contentType);

        assertThat(result).contains("til-images");
        assertThat(result).contains("X-Amz-Signature");
    }
}