package com.Rootin.domain.s3.controller;

import com.Rootin.domain.garden.service.PotService;
import com.Rootin.domain.s3.dto.PresignedUrlRequest;
import com.Rootin.global.config.TestSecurityConfig;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.jwt.JwtUserDetails;
import com.Rootin.global.s3.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PresignedUrlController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PresignedUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private PotService potService;

    private JwtUserDetails userDetails;

    private static final Long USER_ID = 1L;
    private static final Long POT_ID = 10L;
    private static final String PRESIGNED_URL = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/til-images/1/10/uuid.jpg?X-Amz-Signature=abc";
    private static final String IMAGE_URL = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/til-images/1/10/uuid.jpg";

    @BeforeEach
    void setUp() {
        userDetails = new JwtUserDetails(
                USER_ID,
                "test@rootin.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("유효한 JPEG 요청이면 presignedUrl과 imageUrl을 포함한 200 OK를 반환한다")
    void getPresignedUrl_jpeg_success() throws Exception {
        PresignedUrlRequest request = buildRequest("image/jpeg", POT_ID);

        doNothing().when(potService).validateOwnership(USER_ID, POT_ID);
        given(s3Service.generatePresignedPutUrl(anyString(), eq("image/jpeg"))).willReturn(PRESIGNED_URL);
        given(s3Service.getFileUrl(anyString())).willReturn(IMAGE_URL);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.presignedUrl").value(containsString("X-Amz-Signature")))
                .andExpect(jsonPath("$.data.imageUrl").value(containsString("til-images")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/webp"})
    @DisplayName("image/png, image/webp contentType도 정상 처리되어 200 OK를 반환한다")
    void getPresignedUrl_supportedContentTypes_success(String contentType) throws Exception {
        PresignedUrlRequest request = buildRequest(contentType, POT_ID);

        doNothing().when(potService).validateOwnership(USER_ID, POT_ID);
        given(s3Service.generatePresignedPutUrl(anyString(), eq(contentType))).willReturn(PRESIGNED_URL);
        given(s3Service.getFileUrl(anyString())).willReturn(IMAGE_URL);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("contentType이 blank이면 @NotBlank 검증에 실패해 400 Bad Request를 반환한다")
    void getPresignedUrl_blankContentType_returns400() throws Exception {
        PresignedUrlRequest request = buildRequest("", POT_ID);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("potId가 null이면 @NotNull 검증에 실패해 400 Bad Request를 반환한다")
    void getPresignedUrl_nullPotId_returns400() throws Exception {
        String requestJson = """
                { "contentType": "image/jpeg" }
                """;

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 contentType(image/gif)이면 400 Bad Request를 반환한다")
    void getPresignedUrl_unsupportedContentType_returns400() throws Exception {
        PresignedUrlRequest request = buildRequest("image/gif", POT_ID);

        doNothing().when(potService).validateOwnership(USER_ID, POT_ID);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("화분 소유자가 아닌 경우 403 Forbidden을 반환한다")
    void getPresignedUrl_notOwner_returns403() throws Exception {
        PresignedUrlRequest request = buildRequest("image/jpeg", POT_ID);

        doThrow(CustomException.forbidden("해당 화분에 접근할 권한이 없습니다."))
                .when(potService).validateOwnership(USER_ID, POT_ID);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 화분 ID이면 404 Not Found를 반환한다")
    void getPresignedUrl_potNotFound_returns404() throws Exception {
        Long unknownPotId = 999L;
        PresignedUrlRequest request = buildRequest("image/jpeg", unknownPotId);

        doThrow(CustomException.notFound("존재하지 않는 화분입니다. ID: " + unknownPotId))
                .when(potService).validateOwnership(USER_ID, unknownPotId);

        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("요청 바디가 없으면 400 Bad Request를 반환한다")
    void getPresignedUrl_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/tils/image/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private PresignedUrlRequest buildRequest(String contentType, Long potId) {
        PresignedUrlRequest request = new PresignedUrlRequest();
        request.setContentType(contentType);
        request.setPotId(potId);
        return request;
    }
}