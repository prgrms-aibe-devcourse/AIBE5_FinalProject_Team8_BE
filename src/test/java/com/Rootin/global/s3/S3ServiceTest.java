package com.Rootin.global.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private S3Service s3Service;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Presigner);
        ReflectionTestUtils.setField(s3Service, "bucket", "rootin-bucket");
        ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("generatePresignedPutUrl — S3Presigner 호출 후 URL 문자열 반환")
    void generatePresignedPutUrl_success() throws MalformedURLException {
        // given
        String objectKey = "profile-images/1/uuid.jpg";
        String contentType = "image/jpeg";
        URL fakeUrl = new URL("https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey + "?X-Amz-Signature=abc");

        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(fakeUrl);

        // when
        String result = s3Service.generatePresignedPutUrl(objectKey, contentType);

        // then
        assertThat(result).isEqualTo(fakeUrl.toString());
        assertThat(result).contains("X-Amz-Signature");
    }

    @Test
    @DisplayName("getFileUrl — bucket/region/objectKey 조합으로 S3 공개 URL 반환")
    void getFileUrl_success() {
        // given
        String objectKey = "profile-images/1/uuid.jpg";

        // when
        String result = s3Service.getFileUrl(objectKey);

        // then
        assertThat(result).isEqualTo(
                "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/profile-images/1/uuid.jpg"
        );
    }
}
