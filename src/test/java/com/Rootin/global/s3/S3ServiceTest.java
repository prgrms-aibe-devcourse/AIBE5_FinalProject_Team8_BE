package com.Rootin.global.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("rootin-bucket");

        S3Properties props = new S3Properties();
        props.setRegion("ap-northeast-2");
        props.setS3(s3);

        s3Service = new S3Service(s3Presigner, props);
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
}