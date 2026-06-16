package com.Rootin.global.s3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3PropertiesTest {

    @Test
    @DisplayName("기본 리전은 ap-northeast-2이다")
    void defaultRegion_isApNortheast2() {
        S3Properties props = new S3Properties();
        assertThat(props.getRegion()).isEqualTo("ap-northeast-2");
    }

    @Test
    @DisplayName("기본 자격증명 accessKey는 빈 문자열이다")
    void defaultAccessKey_isEmpty() {
        S3Properties props = new S3Properties();
        assertThat(props.getCredentials().getAccessKey()).isEmpty();
    }

    @Test
    @DisplayName("기본 자격증명 secretKey는 빈 문자열이다")
    void defaultSecretKey_isEmpty() {
        S3Properties props = new S3Properties();
        assertThat(props.getCredentials().getSecretKey()).isEmpty();
    }

    @Test
    @DisplayName("기본 S3 endpoint는 빈 문자열이다")
    void defaultEndpoint_isEmpty() {
        S3Properties props = new S3Properties();
        assertThat(props.getS3().getEndpoint()).isEmpty();
    }

    @Test
    @DisplayName("버킷 이름을 설정하면 정상적으로 조회된다")
    void setBucket_isRetrievable() {
        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setBucket("my-test-bucket");

        S3Properties props = new S3Properties();
        props.setS3(s3);

        assertThat(props.getS3().getBucket()).isEqualTo("my-test-bucket");
    }

    @Test
    @DisplayName("리전을 us-east-1로 설정하면 정상적으로 조회된다")
    void setRegion_isRetrievable() {
        S3Properties props = new S3Properties();
        props.setRegion("us-east-1");

        assertThat(props.getRegion()).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("accessKey와 secretKey를 설정하면 정상적으로 조회된다")
    void setCredentials_areRetrievable() {
        S3Properties.Credentials creds = new S3Properties.Credentials();
        creds.setAccessKey("AKIAIOSFODNN7EXAMPLE");
        creds.setSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        S3Properties props = new S3Properties();
        props.setCredentials(creds);

        assertThat(props.getCredentials().getAccessKey()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
        assertThat(props.getCredentials().getSecretKey()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }

    @Test
    @DisplayName("LocalStack endpoint를 설정하면 정상적으로 조회된다")
    void setLocalStackEndpoint_isRetrievable() {
        S3Properties.S3 s3 = new S3Properties.S3();
        s3.setEndpoint("http://localhost:4566");

        S3Properties props = new S3Properties();
        props.setS3(s3);

        assertThat(props.getS3().getEndpoint()).isEqualTo("http://localhost:4566");
    }

    @Test
    @DisplayName("Credentials 중첩 클래스는 독립적으로 인스턴스화된다")
    void credentials_innerClass_instantiatesIndependently() {
        S3Properties.Credentials creds1 = new S3Properties.Credentials();
        S3Properties.Credentials creds2 = new S3Properties.Credentials();

        creds1.setAccessKey("key1");
        creds2.setAccessKey("key2");

        assertThat(creds1.getAccessKey()).isEqualTo("key1");
        assertThat(creds2.getAccessKey()).isEqualTo("key2");
    }

    @Test
    @DisplayName("S3 중첩 클래스는 독립적으로 인스턴스화된다")
    void s3_innerClass_instantiatesIndependently() {
        S3Properties.S3 s3a = new S3Properties.S3();
        S3Properties.S3 s3b = new S3Properties.S3();

        s3a.setBucket("bucket-a");
        s3b.setBucket("bucket-b");

        assertThat(s3a.getBucket()).isEqualTo("bucket-a");
        assertThat(s3b.getBucket()).isEqualTo("bucket-b");
    }
}