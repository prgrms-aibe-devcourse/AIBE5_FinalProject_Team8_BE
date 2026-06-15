package com.Rootin.global.s3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class S3ConfigTest {

    private final S3Config s3Config = new S3Config();

    @Test
    @DisplayName("명시적 accessKey/secretKey가 있으면 StaticCredentialsProvider로 S3Presigner 빈이 생성된다")
    void s3Presigner_withExplicitCredentials_createsBean() {
        S3Properties props = buildProps("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", null);

        S3Presigner presigner = s3Config.s3Presigner(props);

        assertThat(presigner).isNotNull();
        presigner.close();
    }

    @Test
    @DisplayName("accessKey가 빈 문자열이면 DefaultCredentialsProvider로 S3Presigner 빈이 생성된다")
    void s3Presigner_withEmptyCredentials_createsBean() {
        S3Properties props = buildProps("", "", null);

        // DefaultCredentialsProvider는 lazy 로딩이므로 빈 생성 단계에서 예외가 발생하지 않는다
        assertThatNoException().isThrownBy(() -> {
            S3Presigner presigner = s3Config.s3Presigner(props);
            presigner.close();
        });
    }

    @Test
    @DisplayName("accessKey가 null이면 DefaultCredentialsProvider로 대체된다")
    void s3Presigner_withNullCredentials_createsBean() {
        S3Properties props = buildProps(null, null, null);

        assertThatNoException().isThrownBy(() -> {
            S3Presigner presigner = s3Config.s3Presigner(props);
            presigner.close();
        });
    }

    @Test
    @DisplayName("LocalStack 엔드포인트가 설정되면 path-style 접근으로 S3Presigner 빈이 생성된다")
    void s3Presigner_withLocalStackEndpoint_createsBean() {
        S3Properties props = buildProps("test-key", "test-secret", "http://localhost:4566");

        S3Presigner presigner = s3Config.s3Presigner(props);

        assertThat(presigner).isNotNull();
        presigner.close();
    }

    @Test
    @DisplayName("엔드포인트가 빈 문자열이면 표준 AWS 엔드포인트로 S3Presigner 빈이 생성된다")
    void s3Presigner_withBlankEndpoint_createsStandardBean() {
        S3Properties props = buildProps("test-key", "test-secret", "");

        S3Presigner presigner = s3Config.s3Presigner(props);

        assertThat(presigner).isNotNull();
        presigner.close();
    }

    @Test
    @DisplayName("리전이 us-east-1이어도 정상적으로 S3Presigner 빈이 생성된다")
    void s3Presigner_withDifferentRegion_createsBean() {
        S3Properties.Credentials creds = new S3Properties.Credentials();
        creds.setAccessKey("test-key");
        creds.setSecretKey("test-secret");

        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("us-test-bucket");

        S3Properties props = new S3Properties();
        props.setRegion("us-east-1");
        props.setCredentials(creds);
        props.setS3(s3);

        S3Presigner presigner = s3Config.s3Presigner(props);

        assertThat(presigner).isNotNull();
        presigner.close();
    }

    private S3Properties buildProps(String accessKey, String secretKey, String endpoint) {
        S3Properties.Credentials credentials = new S3Properties.Credentials();
        credentials.setAccessKey(accessKey);
        credentials.setSecretKey(secretKey);

        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("test-bucket");
        if (endpoint != null) {
            s3.setEndpoint(endpoint);
        }

        S3Properties props = new S3Properties();
        props.setRegion("ap-northeast-2");
        props.setCredentials(credentials);
        props.setS3(s3);

        return props;
    }
}