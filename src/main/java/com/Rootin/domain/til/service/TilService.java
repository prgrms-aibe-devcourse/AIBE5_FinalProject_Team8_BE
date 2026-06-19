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
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public TilResponse create(Long userId, TilCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        // TIL 발행은 곧 화분 경험치 변경으로 이어집니다.
        // 같은 화분에 여러 요청이 동시에 들어오면 totalExp 계산이 꼬일 수 있으므로,
        // 쓰기 흐름에서는 비관적 락으로 Pot을 조회해 한 번에 하나의 트랜잭션만 경험치를 수정하게 합니다.
        Pot pot = potRepository.findByIdWithLock(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));
        validatePotOwner(pot, userId);

        // Til.create()는 PUBLISHED 상태의 TIL을 만드는 도메인 생성 메서드입니다.
        // 저장 후 til.getId()가 필요하므로 먼저 save()를 호출한 뒤 물주기 로직을 실행합니다.
        Til til = Til.create(user, request.title(), request.content(), pot);
        tilRepository.save(til);

        syncTags(til, request.tags());

        // TIL 저장 성공 직후, 글자 수 및 이력을 바탕으로 물주기 경험치/포인트/레벨업 비즈니스 로직을 구동합니다.
        // 이 호출이 Phase 2 경험치 시스템과 TIL 도메인을 연결하는 핵심 지점입니다.
        // 경험치 산정 글자 수는 HTML 원문 길이가 아니라 태그·공백을 제외한 순수 텍스트 기준으로 계산합니다.
        // (서식만 추가해도 경험치가 늘어나는 문제 방지 + 에디터 예상 경험치와 기준 일치)
        int contentLength = TilContentLength.countVisibleCharacters(request.content());
        experienceService.applyWatering(userId, pot, contentLength, til.getId());

        return TilResponse.from(til);
    }

    @Transactional(readOnly = true)
    public TilResponse findById(Long tilId, Long userId) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);
        return TilResponse.from(til);
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

        return tils.map(TilResponse::from);
    }

    @Transactional
    public TilResponse update(Long tilId, Long userId, TilUpdateRequest request) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);

        til.update(request.title(), request.content());
        syncTags(til, request.tags());

        return TilResponse.from(til);
    }

    @Transactional
    public void delete(Long tilId, Long userId) {
        Til til = getTilOrThrow(tilId);
        validateOwner(til, userId);
        // FK 참조 테이블을 먼저 정리한 뒤 TIL 삭제
        // - ai_result_til: 중간 테이블만 제거, ai_results 레코드는 유지
        // - watering_log: TIL에 귀속된 물주기 이력 제거 (경험치 중복 방지 unique 제약 해소)
        aiResultTilRepository.deleteByTilId(tilId);
        wateringLogRepository.deleteByPostId(tilId);
        tilRepository.delete(til);
    }

    @Transactional
    public TilResponse saveDraft(Long userId, DraftSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        Pot pot = potRepository.findById(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));
        validatePotOwner(pot, userId);

        // 임시저장은 DRAFT 상태이므로 경험치/물주기를 발생시키지 않습니다.
        // 이미 같은 화분에 임시저장 글이 있으면 새로 만들지 않고 기존 글을 갱신합니다.
        Til til = tilRepository.findFirstByUserIdAndPotIdAndStatus(userId, request.potId(), PostStatus.DRAFT)
                .map(existing -> {
                    existing.update(request.title(), request.content());
                    return existing;
                })
                .orElseGet(() -> tilRepository.save(Til.createDraft(user, request.title(), request.content(), pot)));

        syncTags(til, request.tags());

        return TilResponse.from(til);
    }

    @Transactional(readOnly = true)
    public TilResponse getDraft(Long userId, Long potId) {
        Til til = tilRepository.findFirstByUserIdAndPotIdAndStatus(userId, potId, PostStatus.DRAFT)
                .orElseThrow(() -> CustomException.notFound("임시저장된 TIL이 없습니다."));
        return TilResponse.from(til);
    }

    @Transactional
    public void deleteDraft(Long userId, Long potId) {
        Til til = tilRepository.findFirstByUserIdAndPotIdAndStatus(userId, potId, PostStatus.DRAFT)
                .orElseThrow(() -> CustomException.notFound("임시저장된 TIL이 없습니다."));
        validateOwner(til, userId);
        // DRAFT 상태 TIL은 watering_log·ai_result_til 레코드가 생성되지 않으므로 별도 FK 정리 불필요.
        // 향후 AI 분석 또는 물주기가 DRAFT 단계까지 확장될 경우 delete()와 동일한 패턴 적용 필요.
        tilRepository.delete(til);
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
            throw CustomException.forbidden("해당 화분에 대한 권한이 없습니다.");
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
