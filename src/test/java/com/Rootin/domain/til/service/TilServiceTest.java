// TilService 단위 테스트
// [S3 이미지 업로드 기능 추가] PostImageRepository 목 추가, 이미지 저장·조회·수정·삭제 시나리오 검증
// 기존 테스트: TilCreateRequest 필드 추가(imageUrls)에 맞춰 생성자 인자 수정
package com.Rootin.domain.til.service;

import com.Rootin.domain.ai.repository.AiResultTilRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.ExperienceService;
import com.Rootin.domain.til.dto.request.DraftSaveRequest;
import com.Rootin.domain.til.dto.request.TilCreateRequest;
import com.Rootin.domain.til.dto.request.TilUpdateRequest;
import com.Rootin.domain.til.dto.response.TilResponse;
import com.Rootin.domain.til.entity.PostImage;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.PostImageRepository;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TilServiceTest {

    @InjectMocks
    private TilService tilService;

    @Mock private TilRepository tilRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private PotRepository potRepository;
    @Mock private ExperienceService experienceService;
    @Mock private AiResultTilRepository aiResultTilRepository;
    @Mock private WateringLogRepository wateringLogRepository;
    @Mock private S3Service s3Service;
    // [S3 이미지 업로드 기능 추가] PostImageRepository 목 추가
    @Mock private PostImageRepository postImageRepository;
    @Mock private TilTagRepository tilTagRepository;

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
        // [S3 이미지 업로드 기능 추가] tilTags 필드 초기화 (TilResponse.from 내부에서 stream() 호출 대비)
        ReflectionTestUtils.setField(til, "tilTags", new ArrayList<>());
    }

    // --- saveDraft() ---

    @Test
    @DisplayName("임시저장 저장 — 기존 초안을 벌크 삭제한 뒤 새 DRAFT를 저장한다")
    void saveDraft_replacesExistingDraftWithBulkDelete() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
        given(userRepository.getReferenceById(1L)).willReturn(owner);
        given(potRepository.getReferenceById(10L)).willReturn(pot);
        given(tilRepository.findIdsByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.DRAFT))
                .willReturn(List.of(100L));
        given(tilRepository.save(any(Til.class))).willAnswer(invocation -> {
            Til saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 200L);
            return saved;
        });
        given(postImageRepository.findByPostIdOrderByImageOrder(200L))
                .willReturn(Collections.emptyList());

        DraftSaveRequest request = new DraftSaveRequest(
                10L,
                "새 임시저장",
                "기존 초안을 대체하는 본문",
                List.of(),
                List.of()
        );

        TilResponse response = tilService.saveDraft(1L, request, null);

        assertThat(response.tilId()).isEqualTo(200L);

        var inOrder = inOrder(postImageRepository, tilTagRepository, tilRepository);
        inOrder.verify(postImageRepository).deleteByPostId(100L);
        inOrder.verify(tilTagRepository).deleteByTilIdIn(List.of(100L));
        inOrder.verify(tilRepository).deleteTilRowsByIds(List.of(100L));
        inOrder.verify(tilRepository).deletePostRowsByIds(List.of(100L));
        inOrder.verify(tilRepository).save(any(Til.class));
        verify(tilRepository, never()).findFirstByUserIdAndPotIdAndStatus(anyLong(), anyLong(), any());
    }

    // --- deleteDraft() ---

    @Test
    @DisplayName("임시저장 삭제 — 같은 화분 쓰기 락 후 이미지, TilTag, Til, Post를 벌크 삭제한다")
    void deleteDraft_success_bulkDeletesDraftRows() {
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findIdsByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.DRAFT))
                .willReturn(List.of(100L, 101L));
        given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                .willReturn(Collections.emptyList());
        given(postImageRepository.findByPostIdOrderByImageOrder(101L))
                .willReturn(Collections.emptyList());

        tilService.deleteDraft(1L, 10L);

        var inOrder = inOrder(postImageRepository, tilTagRepository, tilRepository);
        inOrder.verify(postImageRepository).deleteByPostId(100L);
        inOrder.verify(postImageRepository).deleteByPostId(101L);
        inOrder.verify(tilTagRepository).deleteByTilIdIn(List.of(100L, 101L));
        inOrder.verify(tilRepository).deleteTilRowsByIds(List.of(100L, 101L));
        inOrder.verify(tilRepository).deletePostRowsByIds(List.of(100L, 101L));
    }

    @Test
    @DisplayName("임시저장 삭제 — 삭제할 초안이 없어도 멱등하게 성공한다")
    void deleteDraft_noDraft_isNoop() {
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findIdsByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.DRAFT))
                .willReturn(List.of());

        tilService.deleteDraft(1L, 10L);

        verify(postImageRepository, never()).deleteByPostId(anyLong());
        verify(tilTagRepository, never()).deleteByTilIdIn(anyList());
        verify(tilRepository, never()).deleteTilRowsByIds(anyList());
        verify(tilRepository, never()).deletePostRowsByIds(anyList());
    }

    @Test
    @DisplayName("타인 화분 임시저장 삭제 시도 → 403")
    void deleteDraft_forbidden_whenPotOwnerMismatch() {
        given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));

        assertThatThrownBy(() -> tilService.deleteDraft(2L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(tilRepository, never()).findIdsByUserIdAndPotIdAndStatus(anyLong(), anyLong(), any());
        verifyNoInteractions(tilTagRepository);
    }

    // --- create() ---

    @Nested
    @DisplayName("TIL 작성 (create)")
    class CreateTests {

        @Test
        @DisplayName("이미지 없이 TIL 작성 → S3 업로드 미호출, images 빈 목록 반환")
        void create_withoutImage_thumbnailUrlIsNull() {
            // [수정] TilCreateRequest 5번째 인자(imageUrls) null 추가 (기존 4인자 → 5인자)
            TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of(), null);

            given(userRepository.existsById(1L)).willReturn(true);
            given(userRepository.getReferenceById(1L)).willReturn(owner);
            given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
            given(tilRepository.save(any(Til.class))).willReturn(til);
            // [추가] create() 마지막에 이미지 조회 → 빈 목록 반환
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            TilResponse response = tilService.create(1L, request, null);

            verify(s3Service, never()).uploadFile(any(), any());
            verify(postImageRepository, never()).saveAll(anyList());
            assertThat(response.images()).isEmpty();
        }

        @Test
        @DisplayName("썸네일 이미지 포함 TIL 작성 → S3 uploadFile 호출")
        void create_withThumbnailImage_uploadsToS3() {
            TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of(), null);
            MockMultipartFile image = new MockMultipartFile(
                    "image", "thumb.jpg", "image/jpeg", "bytes".getBytes());
            String expectedUrl = "https://rootin-bucket.s3.ap-northeast-2.amazonaws.com/til-images/1/10/uuid.jpg";

            given(userRepository.existsById(1L)).willReturn(true);
            given(userRepository.getReferenceById(1L)).willReturn(owner);
            given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
            given(s3Service.uploadFile(any(), any())).willReturn(expectedUrl);
            given(tilRepository.save(any(Til.class))).willReturn(til);
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            tilService.create(1L, request, image);

            verify(s3Service).uploadFile(any(), any());
        }

        @Test
        @DisplayName("지원하지 않는 이미지 타입 → 400 예외, S3 업로드 미호출")
        void create_unsupportedImageType_throws400() {
            TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of(), null);
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.gif", "image/gif", "bytes".getBytes());

            given(userRepository.existsById(1L)).willReturn(true);
            given(userRepository.getReferenceById(1L)).willReturn(owner);
            given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));

            assertThatThrownBy(() -> tilService.create(1L, request, image))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));

            verify(s3Service, never()).uploadFile(any(), any());
        }

        @Test
        @DisplayName("[신규] imageUrls 포함 TIL 작성 → til_images saveAll 호출, 응답에 이미지 포함")
        void create_withImageUrls_savesImageRecords() {
            List<String> urls = List.of(
                    "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/til-images/uuid1/10/0/img1.jpg",
                    "https://team8-rootin-s3.s3.ap-northeast-2.amazonaws.com/til-images/uuid2/10/0/img2.jpg"
            );
            TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of(), urls);

            given(userRepository.existsById(1L)).willReturn(true);
            given(userRepository.getReferenceById(1L)).willReturn(owner);
            given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
            given(tilRepository.save(any(Til.class))).willReturn(til);
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(buildPostImages(100L, urls));

            TilResponse response = tilService.create(1L, request, null);

            verify(postImageRepository).saveAll(anyList());
            assertThat(response.images()).hasSize(2);
            assertThat(response.images().get(0).imageOrder()).isEqualTo(0);
            assertThat(response.images().get(1).imageOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("[신규] imageUrls 빈 리스트 → saveAll 미호출, 이미지 없는 응답 반환")
        void create_withEmptyImageUrls_doesNotSaveImages() {
            TilCreateRequest request = new TilCreateRequest("제목", "내용", 10L, List.of(), List.of());

            given(userRepository.existsById(1L)).willReturn(true);
            given(userRepository.getReferenceById(1L)).willReturn(owner);
            given(potRepository.findByIdWithLock(10L)).willReturn(Optional.of(pot));
            given(tilRepository.save(any(Til.class))).willReturn(til);
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            TilResponse response = tilService.create(1L, request, null);

            verify(postImageRepository, never()).saveAll(anyList());
            assertThat(response.images()).isEmpty();
        }
    }

    // --- findById() ---

    @Nested
    @DisplayName("TIL 단건 조회 (findById)")
    class FindByIdTests {

        @Test
        @DisplayName("[신규] 이미지 있는 TIL 조회 → 응답에 images 목록 포함")
        void findById_returnsImagesInResponse() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));
            List<String> urls = List.of(
                    "https://s3.amazonaws.com/til-images/a/10/100/img1.jpg",
                    "https://s3.amazonaws.com/til-images/b/10/100/img2.jpg"
            );
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(buildPostImages(100L, urls));

            TilResponse response = tilService.findById(100L, 1L);

            assertThat(response.images()).hasSize(2);
            assertThat(response.images().get(0).url())
                    .isEqualTo("https://s3.amazonaws.com/til-images/a/10/100/img1.jpg");
            assertThat(response.images().get(0).imageOrder()).isEqualTo(0);
            assertThat(response.images().get(1).imageOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("[신규] 이미지 없는 TIL 조회 → images 빈 목록 반환")
        void findById_noImages_returnsEmptyList() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            TilResponse response = tilService.findById(100L, 1L);

            assertThat(response.images()).isEmpty();
        }

        @Test
        @DisplayName("[신규] 다른 사용자가 조회 → 403 예외")
        void findById_forbidden_when_not_owner() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));

            assertThatThrownBy(() -> tilService.findById(100L, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("존재하지 않는 TIL 조회 → 404 예외")
        void findById_notFound_throws404() {
            given(tilRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tilService.findById(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // --- update() ---

    @Nested
    @DisplayName("TIL 수정 (update)")
    class UpdateTests {

        @Test
        @DisplayName("[신규] deletedImageIds 지정 → S3 삭제 + DB 삭제 호출")
        void update_withDeletedImageIds_deletesFromS3AndDb() {
            List<Long> deletedIds = List.of(1L, 2L);
            TilUpdateRequest request = new TilUpdateRequest("제목", "내용", List.of(), null, deletedIds);

            given(tilRepository.findById(100L)).willReturn(Optional.of(til));

            List<PostImage> toDelete = List.of(
                    buildPostImage(1L, 100L, "https://s3.amazonaws.com/til-images/a/img1.jpg", 0),
                    buildPostImage(2L, 100L, "https://s3.amazonaws.com/til-images/b/img2.jpg", 1)
            );
            given(postImageRepository.findAllById(deletedIds)).willReturn(toDelete);
            // [버그 수정 반영] remainingImages 조회(nextOrder 계산용) + 최종 조회 → 각 1회씩 2회 호출
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList())  // 1st: nextOrder 계산 (max=-1 → nextOrder=0)
                    .willReturn(Collections.emptyList()); // 2nd: 최종 이미지 목록

            tilService.update(100L, 1L, request, null);

            verify(s3Service, times(2)).deleteFileByUrl(any());
            verify(postImageRepository).deleteAllByIdIn(deletedIds);
        }

        @Test
        @DisplayName("[신규] imageUrls 전달 → 기존 이미지 수 이후로 imageOrder 이어붙이기")
        void update_withNewImageUrls_appendsAfterExistingImages() {
            List<String> newUrls = List.of(
                    "https://s3.amazonaws.com/til-images/new/img3.jpg",
                    "https://s3.amazonaws.com/til-images/new/img4.jpg"
            );
            TilUpdateRequest request = new TilUpdateRequest("제목", "내용", List.of(), newUrls, null);

            given(tilRepository.findById(100L)).willReturn(Optional.of(til));

            List<PostImage> existing = List.of(
                    buildPostImage(3L, 100L, "https://s3.amazonaws.com/til-images/old/img0.jpg", 0)
            );
            // 1st call: remainingImages(nextOrder 계산 → max(0)+1=1), 2nd call: 최종 목록
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(existing)
                    .willReturn(List.of(
                            buildPostImage(3L, 100L, "https://s3.amazonaws.com/til-images/old/img0.jpg", 0),
                            buildPostImage(4L, 100L, newUrls.get(0), 1),
                            buildPostImage(5L, 100L, newUrls.get(1), 2)
                    ));

            TilResponse response = tilService.update(100L, 1L, request, null);

            verify(postImageRepository).saveAll(anyList());
            assertThat(response.images()).hasSize(3);
        }

        @Test
        @DisplayName("[신규] imageUrls/deletedImageIds 모두 null → 이미지 조작 없음")
        void update_withoutImageChanges_noImageOperations() {
            TilUpdateRequest request = new TilUpdateRequest("제목", "내용", List.of(), null, null);

            given(tilRepository.findById(100L)).willReturn(Optional.of(til));
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            tilService.update(100L, 1L, request, null);

            verify(s3Service, never()).deleteFileByUrl(any());
            verify(postImageRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("[신규] 소유자가 아닌 사용자 수정 → 403 예외")
        void update_forbidden_when_not_owner() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));
            TilUpdateRequest request = new TilUpdateRequest("제목", "내용", List.of(), null, null);

            assertThatThrownBy(() -> tilService.update(100L, 2L, request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    // --- delete() ---

    @Nested
    @DisplayName("TIL 삭제 (delete)")
    class DeleteTests {

        @Test
        @DisplayName("[신규] TIL 삭제 시 S3 삭제 → DB 이미지 삭제 → TIL 삭제 순서 보장")
        void delete_cleansUpImages_beforeDeletingTil() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));

            List<PostImage> images = List.of(
                    buildPostImage(1L, 100L, "https://s3.amazonaws.com/til-images/a/img1.jpg", 0),
                    buildPostImage(2L, 100L, "https://s3.amazonaws.com/til-images/b/img2.jpg", 1)
            );
            given(postImageRepository.findByPostIdOrderByImageOrder(100L)).willReturn(images);

            tilService.delete(100L, 1L);

            var inOrder = inOrder(s3Service, postImageRepository, tilRepository);
            inOrder.verify(s3Service, times(2)).deleteFileByUrl(any());
            inOrder.verify(postImageRepository).deleteByPostId(100L);
            inOrder.verify(tilRepository).delete(til);
        }

        @Test
        @DisplayName("[신규] 이미지 없는 TIL 삭제 → S3 삭제 미호출, TIL 정상 삭제")
        void delete_withNoImages_skipS3Delete() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));
            given(postImageRepository.findByPostIdOrderByImageOrder(100L))
                    .willReturn(Collections.emptyList());

            tilService.delete(100L, 1L);

            verify(s3Service, never()).deleteFileByUrl(any());
            verify(tilRepository).delete(til);
        }

        @Test
        @DisplayName("[신규] 소유자가 아닌 사용자 삭제 → 403 예외, 실제 삭제 미호출")
        void delete_forbidden_when_not_owner() {
            given(tilRepository.findById(100L)).willReturn(Optional.of(til));

            assertThatThrownBy(() -> tilService.delete(100L, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));

            verify(tilRepository, never()).delete(any());
            verify(postImageRepository, never()).deleteByPostId(any());
        }

        @Test
        @DisplayName("존재하지 않는 TIL 삭제 → 404 예외")
        void delete_notFound_throws404() {
            given(tilRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tilService.delete(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // --- 헬퍼 메서드 ---

    /**
     * URL 목록으로 PostImage 리스트를 생성한다. imageOrder = 0-indexed, id = 인덱스 + 1.
     */
    private List<PostImage> buildPostImages(Long postId, List<String> urls) {
        List<PostImage> result = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            result.add(buildPostImage((long) (i + 1), postId, urls.get(i), i));
        }
        return result;
    }

    /**
     * 단일 PostImage 생성. ReflectionTestUtils로 private id 필드 주입.
     */
    private PostImage buildPostImage(Long id, Long postId, String url, int imageOrder) {
        PostImage image = PostImage.of(postId, url, imageOrder);
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
