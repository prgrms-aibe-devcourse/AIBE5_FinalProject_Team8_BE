// TilService 단위 테스트: TIL 작성 시 이미지 유무에 따른 S3 업로드 호출 여부와 삭제 순서를 검증한다
package com.Rootin.domain.til.service;

import com.Rootin.domain.ai.repository.AiResultTilRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.ExperienceService;
import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TilServiceTest {

    @InjectMocks
    private TilService tilService;

    @Mock
    private TilRepository tilRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PotRepository potRepository;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private AiResultTilRepository aiResultTilRepository;

    @Mock
    private WateringLogRepository wateringLogRepository;

    @Mock
    private S3Service s3Service;

    private User owner;
    private Pot pot;
    private Til til;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);

        pot = Pot.builder()
                .userId(1L)
                .title("테스트 화분")
                .level(1)
                .totalExp(0)
                .build();
        ReflectionTestUtils.setField(pot, "id", 10L);

        til = new Til();
        ReflectionTestUtils.setField(til, "id", 100L);
        ReflectionTestUtils.setField(til, "user", owner);
        ReflectionTestUtils.setField(til, "pot", pot);
        ReflectionTestUtils.setField(til, "status", PostStatus.PUBLISHED);
    }

    // ─── create() ────────────────────────────────────────────────────

    @Test
    @DisplayName("이미지 없이 TIL 작성 → thumbnailUrl null, S3 업로드 미호출")
    void create_withoutImage_thumbnailUrlIsNull() {
        TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of());

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
        given(tilRepository.save(any(Til.class))).willReturn(til);

        tilService.create(1L, request, null);

        verify(s3Service, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("이미지 포함 TIL 작성 → S3 uploadFile 호출, thumbnailUrl 반환")
    void create_withImage_uploadsToS3() {
        TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of());
        MockMultipartFile image = new MockMultipartFile(
                "image", "thumb.jpg", "image/jpeg", "bytes".getBytes()
        );
        String expectedUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/til-images/1/10/uuid.jpg";

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
        given(s3Service.uploadFile(any(), any())).willReturn(expectedUrl);
        given(tilRepository.save(any(Til.class))).willReturn(til);

        tilService.create(1L, request, image);

        verify(s3Service).uploadFile(any(), any());
    }

    @Test
    @DisplayName("지원하지 않는 이미지 타입 → 400 예외, S3 업로드 미호출")
    void create_unsupportedImageType_throws400() {
        TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of());
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.gif", "image/gif", "bytes".getBytes()
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));

        assertThatThrownBy(() -> tilService.create(1L, request, image))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(s3Service, never()).uploadFile(any(), any());
    }

    // ─── delete() ────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 삭제 — aiResultTil·wateringLog 정리 후 til 삭제, 삭제 순서 보장")
    void delete_success_removesRelatedRecordsInOrder() {
        given(tilRepository.findById(100L)).willReturn(Optional.of(til));

        tilService.delete(100L, 1L);

        // aiResultTil → wateringLog → til 순서 검증
        var inOrder = inOrder(aiResultTilRepository, wateringLogRepository, tilRepository);
        inOrder.verify(aiResultTilRepository).deleteByTilId(100L);
        inOrder.verify(wateringLogRepository).deleteByPostId(100L);
        inOrder.verify(tilRepository).delete(til);
    }

    @Test
    @DisplayName("타인 TIL 삭제 시도 → 403")
    void delete_forbidden_when_not_owner() {
        given(tilRepository.findById(100L)).willReturn(Optional.of(til));

        assertThatThrownBy(() -> tilService.delete(100L, 2L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(aiResultTilRepository, wateringLogRepository);
        verify(tilRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 tilId 삭제 → 404")
    void delete_notFound_when_til_not_exists() {
        given(tilRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tilService.delete(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(aiResultTilRepository, wateringLogRepository);
        verify(tilRepository, never()).delete(any());
    }

}
