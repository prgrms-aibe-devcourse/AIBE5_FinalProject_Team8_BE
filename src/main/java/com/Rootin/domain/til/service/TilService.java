// TIL 비즈니스 로직: TIL 작성 시 썸네일 이미지를 S3에 업로드하고 URL을 저장하는 흐름을 담당한다
// [S3 이미지 업로드 기능 추가] 본문 이미지(PostImage) 저장·수정·삭제 로직 추가
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
import com.Rootin.domain.til.entity.*;
import com.Rootin.domain.til.repository.PostImageRepository;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.til.util.TilContentLength;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.s3.S3Service;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TilService {

    private final TilRepository tilRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final PotRepository potRepository;
    private final ExperienceService experienceService;
    private final AiResultTilRepository aiResultTilRepository;
    private final WateringLogRepository wateringLogRepository;
    private final S3Service s3Service;
    // [S3 이미지 업로드 기능 추가] 본문 이미지 레코드 관리를 위한 리포지토리
    private final PostImageRepository postImageRepository;
    private final TilTagRepository tilTagRepository;

    @Transactional
    public TilResponse create(Long userId, TilCreateRequest request, MultipartFile thumbnailImage) {
        // 경험치 산정 글자 수는 HTML 원문 길이가 아니라 태그·공백을 제외한 순수 텍스트 기준으로 계산합니다.
        // 서식 태그만 있는 본문은 @NotBlank를 통과할 수 있지만, 사용자가 실제로 작성한 내용으로 보지 않습니다.
        int contentLength = TilContentLength.countVisibleCharacters(request.content());
        if (contentLength <= 0) {
            throw CustomException.badRequest("본문은 한 글자 이상 입력해주세요.");
        }

        if (!userRepository.existsById(userId)) {
            throw CustomException.of(ErrorCode.USER_NOT_FOUND);
        }
        User user = userRepository.getReferenceById(userId);

        // TIL 발행은 곧 화분 경험치 변경으로 이어집니다.
        // 같은 화분에 여러 요청이 동시에 들어오면 totalExp 계산이 꼬일 수 있으므로,
        // 쓰기 흐름에서는 비관적 락으로 Pot을 조회해 한 번에 하나의 트랜잭션만 경험치를 수정하게 합니다.
        Pot pot = potRepository.findByIdWithLock(request.potId())
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));
        validatePotOwner(pot, userId);

        // 썸네일 이미지가 첨부된 경우 S3에 업로드하고 URL을 저장합니다.
        String thumbnailUrl = uploadThumbnailIfPresent(thumbnailImage, userId, request.potId());

        // Til.create()는 PUBLISHED 상태의 TIL을 만드는 도메인 생성 메서드입니다.
        // [버그 수정] save() 반환값을 변수에 재할당해야 JPA가 할당한 id를 얻을 수 있다.
        // 기존: tilRepository.save(til) 후 til.getId() == null (Mockito 환경에서 원본 객체 id 미설정)
        // 수정: til = tilRepository.save(til)로 JPA/Mock이 반환하는 영속 객체를 사용한다.
        Til til = tilRepository.save(Til.create(user, request.title(), request.content(), pot, thumbnailUrl));

        syncTags(til, request.tags());

        // [S3 이미지 업로드 기능 추가] TIL 저장 후 본문 이미지 URL 목록을 til_images 테이블에 저장한다.
        // tilId가 확정된 이후에 호출해야 PostImage.postId가 올바르게 세팅된다.
        saveImages(til.getId(), request.imageUrls());

        // TIL 저장 성공 직후, 글자 수 및 이력을 바탕으로 물주기 경험치/포인트/레벨업 비즈니스 로직을 구동합니다.
        // 이 호출이 Phase 2 경험치 시스템과 TIL 도메인을 연결하는 핵심 지점입니다.
        experienceService.applyWatering(userId, pot, contentLength, til.getId());

        // [S3 이미지 업로드 기능 추가] 저장된 이미지 목록을 응답에 포함하여 반환
        List<PostImage> savedImages = postImageRepository.findByPostIdOrderByImageOrder(til.getId());
        return TilResponse.from(til, savedImages);
    }

    @Transactional(readOnly = true)
    public TilResponse findById(Long tilId, Long userId) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);
        // [S3 이미지 업로드 기능 추가] TIL 조회 시 연관 이미지 목록도 함께 반환한다.
        // 클라이언트가 수정 화면 진입 시 기존 이미지를 복원하는 데 사용한다.
        List<PostImage> images = postImageRepository.findByPostIdOrderByImageOrder(tilId);
        return TilResponse.from(til, images);
    }

    @Transactional(readOnly = true)
    public Page<TilResponse> findMyTils(Long userId, Long potId, int page, int size, String sort,
                                        String keyword, String tag) {
        Sort.Direction direction = "oldest".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        boolean hasFilter = (keyword != null && !keyword.isBlank()) || (tag != null && !tag.isBlank());
        Page<Til> tils;
        if (hasFilter) {
            tils = tilRepository.findByFilters(userId, PostStatus.PUBLISHED, potId, keyword, tag, pageable);
        } else if (potId != null) {
            tils = tilRepository.findByUserIdAndPotIdAndStatus(userId, potId, PostStatus.PUBLISHED, pageable);
        } else {
            tils = tilRepository.findByUserIdAndStatus(userId, PostStatus.PUBLISHED, pageable);
        }

        // 목록 조회에서는 이미지가 필요 없으므로 빈 이미지 목록으로 반환 (N+1 방지)
        return tils.map(TilResponse::from);
    }

    @Transactional
    public TilResponse update(Long tilId, Long userId, TilUpdateRequest request, MultipartFile thumbnailImage) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);

        til.update(request.title(), request.content());
        syncTags(til, request.tags());

        if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            String newThumbnailUrl = uploadThumbnailIfPresent(thumbnailImage, userId, til.getPot().getId());
            til.updateThumbnailUrl(newThumbnailUrl);
        }

        // [S3 이미지 업로드 기능 추가] 삭제 요청된 이미지 처리: DB 레코드 삭제 + S3 파일 삭제
        deleteRequestedImages(request.deletedImageIds());

        // [S3 이미지 업로드 기능 추가] 새로 추가된 이미지 URL 목록을 til_images에 저장한다.
        // [버그 수정] existingCount(남은 개수) 대신 max(imageOrder)+1을 startOrder로 사용한다.
        // 이유: 삭제 후 남은 이미지의 imageOrder가 0,2처럼 불연속일 수 있어
        //       existingCount(=1)로 시작하면 기존 order=2와 충돌하는 중복이 발생한다.
        List<PostImage> remainingImages = postImageRepository.findByPostIdOrderByImageOrder(tilId);
        int nextOrder = remainingImages.stream()
                .mapToInt(PostImage::getImageOrder)
                .max()
                .orElse(-1) + 1;
        saveImages(tilId, request.imageUrls(), nextOrder);

        // [S3 이미지 업로드 기능 추가] 최종 이미지 목록을 조회하여 응답에 포함
        List<PostImage> updatedImages = postImageRepository.findByPostIdOrderByImageOrder(tilId);
        return TilResponse.from(til, updatedImages);
    }

    @Transactional
    public void delete(Long tilId, Long userId) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);
        // FK 참조 테이블을 먼저 정리한 뒤 TIL 삭제
        // - ai_result_til: 중간 테이블만 제거, ai_results 레코드는 유지
        // - watering_log: TIL에 귀속된 물주기 이력 제거 (경험치 중복 방지 unique 제약 해소)
        // [S3 이미지 업로드 기능 추가] til_images: DB 레코드 삭제 + S3 파일 일괄 삭제
        deleteAllImagesForTil(tilId);
        aiResultTilRepository.deleteByTilId(tilId);
        wateringLogRepository.deleteByPostId(tilId);
        tilRepository.delete(til);
    }

    @Transactional
    public TilResponse saveDraft(Long userId, DraftSaveRequest request, MultipartFile thumbnailImage) {
        if (!userRepository.existsById(userId)) {
            throw CustomException.of(ErrorCode.USER_NOT_FOUND);
        }
        // 같은 화분의 임시저장 저장/삭제가 동시에 들어오면 DRAFT 레코드가 중복 생성되거나
        // 같은 TilTag를 여러 트랜잭션이 동시에 삭제할 수 있습니다.
        // 발행 전 초안도 "화분당 하나" 정책이므로 pot row lock으로 같은 화분의 draft 쓰기를 직렬화합니다.
        Pot pot = potRepository.findByIdWithLock(request.potId())
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));
        validatePotOwner(pot, userId);

        // 썸네일 이미지가 첨부된 경우 S3에 업로드하고 URL을 저장합니다.
        String thumbnailUrl = uploadThumbnailIfPresent(thumbnailImage, userId, request.potId());

        // 임시저장은 화분당 최신 스냅샷 하나만 유지합니다.
        // 기존 draft 엔티티를 갱신하며 tilTags 컬렉션을 clear()하면 동시 저장/삭제 부하에서
        // Hibernate orphanRemoval 경로가 이미 삭제된 TilTag를 다시 지우려다 낙관락 예외를 만들 수 있습니다.
        // DRAFT는 발행 전 데이터라 외부 이력과 연결되지 않으므로 기존 초안을 벌크 삭제한 뒤 새 초안을 저장합니다.
        deleteDraftRows(userId, request.potId(), false);

        // 벌크 삭제는 영속성 컨텍스트를 비우므로 새 초안 저장에는 관리되는 참조를 다시 사용합니다.
        User user = userRepository.getReferenceById(userId);
        Pot potRef = potRepository.getReferenceById(request.potId());
        Til til = tilRepository.save(Til.createDraft(user, request.title(), request.content(), potRef, thumbnailUrl));

        syncTags(til, request.tags());
        saveImages(til.getId(), request.imageUrls());

        // [S3 이미지 업로드 기능 추가] 저장된 이미지 목록 포함 반환
        List<PostImage> savedImages = postImageRepository.findByPostIdOrderByImageOrder(til.getId());
        return TilResponse.from(til, savedImages);
    }

    @Transactional(readOnly = true)
    public TilResponse getDraft(Long userId, Long potId) {
        Til til = tilRepository.findFirstByUserIdAndPotIdAndStatus(userId, potId, PostStatus.DRAFT)
                .orElseThrow(() -> CustomException.notFound("임시저장된 TIL이 없습니다."));
        // [S3 이미지 업로드 기능 추가] 임시저장 조회 시에도 이미지 목록 반환
        List<PostImage> images = postImageRepository.findByPostIdOrderByImageOrder(til.getId());
        return TilResponse.from(til, images);
    }

    @Transactional
    public void deleteDraft(Long userId, Long potId) {
        Pot pot = potRepository.findByIdWithLock(potId)
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));
        validatePotOwner(pot, userId);

        deleteDraftRows(userId, potId, true);
    }

    /**
     * 썸네일 이미지가 존재하면 S3에 업로드하고 URL을 반환합니다.
     * 이미지가 없거나 비어있으면 null을 반환합니다.
     */
    private String uploadThumbnailIfPresent(MultipartFile image, Long userId, Long potId) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        String contentType = image.getContentType();
        String ext = switch (contentType != null ? contentType : "") {
            case "image/png"  -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw CustomException.badRequest("지원하지 않는 이미지 형식입니다: " + contentType);
        };
        String objectKey = String.format("til-images/%d/%d/%s.%s", userId, potId, UUID.randomUUID(), ext);
        return s3Service.uploadFile(image, objectKey);
    }

    // ─── [S3 이미지 업로드 기능 추가] 이미지 헬퍼 메서드 ────────────────────────────────

    /**
     * imageUrls 목록을 순서대로 til_images 테이블에 저장합니다.
     * imageOrder는 0부터 시작합니다.
     * imageUrls가 null이거나 비어 있으면 아무 작업도 하지 않습니다.
     *
     * @param postId    TIL의 post_id (til_images.post_id FK)
     * @param imageUrls 저장할 이미지 URL 목록 (순서 = imageOrder)
     */
    private void saveImages(Long postId, List<String> imageUrls) {
        saveImages(postId, imageUrls, 0);
    }

    /**
     * imageUrls 목록을 startOrder부터 시작하는 imageOrder로 til_images 테이블에 저장합니다.
     * TIL 수정 시 기존 이미지 개수 이후로 순서를 이어붙일 때 사용합니다.
     *
     * @param postId     TIL의 post_id
     * @param imageUrls  저장할 이미지 URL 목록
     * @param startOrder imageOrder 시작값 (기존 이미지 개수)
     */
    private void saveImages(Long postId, List<String> imageUrls, int startOrder) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<PostImage> images = new java.util.ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            if (url != null && !url.isBlank()) {
                images.add(PostImage.of(postId, url, startOrder + i));
            }
        }
        if (!images.isEmpty()) {
            postImageRepository.saveAll(images);
        }
    }

    /**
     * 삭제 요청된 이미지 ID 목록을 처리합니다.
     * - til_images 테이블에서 해당 레코드를 삭제합니다.
     * - S3에 저장된 실제 파일을 삭제합니다.
     * deletedImageIds가 null이거나 비어 있으면 아무 작업도 하지 않습니다.
     *
     * @param deletedImageIds 삭제할 PostImage.id 목록
     */
    private void deleteRequestedImages(List<Long> deletedImageIds) {
        if (deletedImageIds == null || deletedImageIds.isEmpty()) {
            return;
        }
        // 삭제 전 S3 URL을 먼저 조회하여 S3 파일을 정리한다.
        List<PostImage> toDelete = postImageRepository.findAllById(deletedImageIds);
        for (PostImage image : toDelete) {
            s3Service.deleteFileByUrl(image.getUrl());
        }
        postImageRepository.deleteAllByIdIn(deletedImageIds);
    }

    /**
     * TIL 삭제 시 해당 TIL의 모든 이미지를 S3와 DB에서 일괄 삭제합니다.
     *
     * @param postId 삭제할 TIL의 post_id
     */
    private void deleteAllImagesForTil(Long postId) {
        List<PostImage> images = postImageRepository.findByPostIdOrderByImageOrder(postId);
        for (PostImage image : images) {
            s3Service.deleteFileByUrl(image.getUrl());
        }
        postImageRepository.deleteByPostId(postId);
    }

    private void deleteDraftRows(Long userId, Long potId, boolean deleteS3Images) {
        List<Long> draftIds = tilRepository.findIdsByUserIdAndPotIdAndStatus(userId, potId, PostStatus.DRAFT);
        if (draftIds.isEmpty()) {
            return;
        }

        // 임시저장 삭제는 반복 호출되어도 성공하는 멱등 동작으로 처리합니다.
        // 엔티티 그래프를 로딩해 orphanRemoval로 지우면 동시 DELETE에서 이미 지워진 TilTag를 다시 삭제하며
        // ObjectOptimisticLockingFailureException과 대량 stack trace가 발생할 수 있어 벌크 삭제를 사용합니다.
        // DRAFT 상태 TIL은 watering_log·ai_result_til 레코드가 생성되지 않으므로 별도 FK 정리 불필요.
        if (deleteS3Images) {
            deleteAllImagesForTils(draftIds);
        } else {
            deleteImageRowsForTils(draftIds);
        }
        tilTagRepository.deleteByTilIdIn(draftIds);
        tilRepository.deleteTilRowsByIds(draftIds);
        tilRepository.deletePostRowsByIds(draftIds);
    }

    private void deleteAllImagesForTils(List<Long> postIds) {
        for (Long postId : postIds) {
            deleteAllImagesForTil(postId);
        }
    }

    private void deleteImageRowsForTils(List<Long> postIds) {
        for (Long postId : postIds) {
            postImageRepository.deleteByPostId(postId);
        }
    }

    private Til getTilOrThrow(Long tilId) {
        return tilRepository.findById(tilId)
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));
    }

    private void validateOwner(Til til, Long userId) {
        if (!til.getUser().getId().equals(userId)) {
            throw CustomException.forbidden("해당 TIL에 대한 권한이 없습니다.");
        }
    }

    private void validatePotOwner(Pot pot, Long userId) {
        // TIL 요청의 potId가 "내 화분"인지 확인합니다.
        // 이 검증이 없으면 사용자가 다른 사람의 potId를 넣어 타인의 화분에 글을 쓰거나 경험치를 줄 수 있습니다.
        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }
    }

    /**
     * 사용자가 작성한 TIL의 태그 정보들을 안전하고 최적화된 방식으로 데이터베이스와 동기화합니다.
     *
     * [태그 동기화 벌크 최적화 설계 - 팀원 공유용]
     * 기존에는 사용자가 N개의 태그를 입력하면 루프(for) 안에서 매번 단건 조회(findByName) 쿼리를 N번 실행했습니다. (N+1 문제)
     * 이를 개선하여 한 번의 조회와 한 번의 일괄 저장으로 끝내도록 최적화했습니다.
     *
     * [최적화 동기화 흐름]
     * 1. 1차 벌크 조회: 사용자가 입력한 태그 이름 목록(tagNames)으로 DB에 존재하는 기존 태그를 findByNameIn()으로 단 1번만 쿼리해 옵니다.
     * 2. 인메모리 맵 매핑: 가져온 기존 태그들을 O(1) 조회가 가능하도록 Map<태그이름, Tag> 구조에 캐싱합니다.
     * 3. 신규 태그 필터링: 캐시 맵에 없는, 즉 DB에 가입된 적 없는 신규 태그들만 별도로 리스트로 골라냅니다.
     * 4. 2차 벌크 저장: 신규 태그들이 존재하면 tagRepository.saveAll()을 활용해 단 한 번의 벌크 INSERT 쿼리로 DB에 저장합니다.
     * 5. 연관관계 맺기: 최종적으로 완성된 맵에서 태그 데이터를 추출해 TIL과 태그 매핑 엔티티(TilTag)의 연관관계를 설정합니다.
     *
     * 결과적으로 기존의 쿼리 N+1 횟수를 최대 2번(기존 태그 벌크 조회 1번 + 신규 태그 벌크 저장 1번)으로 완벽하게 제한하여 DB 성능을 개선했습니다.
     *
     * @param til      태그를 맺어줄 대상 TIL 엔티티
     * @param tagNames 사용자가 입력한 태그 이름 목록
     */
    private void syncTags(Til til, List<String> tagNames) {
        // 기존 태그 매핑 내역을 전면 초기화합니다.
        til.getTilTags().clear();

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // 중복된 태그 이름이 전달되었을 때, 신규 태그 벌크 저장 시 unique 제약 조건 위배로
        // 500 예외가 발생하는 현상을 방지하기 위해 distinct() 처리를 수행하여 고유한 리스트로 정제합니다.
        List<String> distinctTagNames = tagNames.stream().distinct().toList();

        // 1. 요청받은 태그 이름들 중 DB에 이미 존재하는 태그들을 단 한 번의 쿼리로 벌크 조회합니다. (N+1 쿼리 방지)
        List<Tag> existingTags = tagRepository.findByNameIn(distinctTagNames);

        // 빠른 조회를 위해 Map<태그이름, Tag> 형태로 임시 캐싱합니다.
        java.util.Map<String, Tag> tagMap = existingTags.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Tag::getName,
                        java.util.function.Function.identity(),
                        (t1, t2) -> t1
                ));

        // 2. 존재하지 않는 신규 태그들만 리스트로 추려냅니다.
        List<Tag> newTagsToSave = new java.util.ArrayList<>();
        for (String name : distinctTagNames) {
            if (!tagMap.containsKey(name)) {
                newTagsToSave.add(Tag.create(name));
            }
        }

        // 3. 신규 태그들이 있다면 saveAll()을 통해 한 번의 INSERT 쿼리로 벌크 저장합니다.
        if (!newTagsToSave.isEmpty()) {
            List<Tag> savedTags = tagRepository.saveAll(newTagsToSave);
            for (Tag tag : savedTags) {
                tagMap.put(tag.getName(), tag);
            }
        }

        // 4. 동기화가 완료된 태그 맵으로부터 데이터를 꺼내어 TIL-태그 매핑 연관 관계 엔티티를 추가합니다.
        for (String name : distinctTagNames) {
            Tag tag = tagMap.get(name);
            if (tag != null) {
                til.getTilTags().add(TilTag.of(til, tag));
            }
        }
    }
}
