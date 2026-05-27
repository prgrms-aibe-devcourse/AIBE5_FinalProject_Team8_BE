package com.Rootin.domain.til.service;

import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
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
    private final com.Rootin.domain.garden.service.ExperienceService experienceService;

    @Transactional
    public TilResponse create(Long userId, TilCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        // 동시성 제어를 위해 비관적 락으로 화분을 안전하게 조회합니다.
        Pot pot = potRepository.findByIdWithLock(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));

        Til til = Til.create(user, request.title(), request.content(), pot);
        tilRepository.save(til);

        syncTags(til, request.tags());

        // TIL 저장 성공 직후, 글자 수 및 이력을 바탕으로 물주기 경험치/포인트/레벨업 비즈니스 로직을 구동합니다.
        int contentLength = request.content() != null ? request.content().length() : 0;
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
    public Page<TilResponse> findMyTils(Long userId, Long potId, int page, int size, String sort) {
        Sort.Direction direction = "oldest".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        Page<Til> tils = potId != null
                ? tilRepository.findByUserIdAndPotIdAndStatus(userId, potId, PostStatus.PUBLISHED, pageable)
                : tilRepository.findByUserIdAndStatus(userId, PostStatus.PUBLISHED, pageable);

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
        tilRepository.delete(til);
    }

    @Transactional
    public TilResponse saveDraft(Long userId, DraftSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        Pot pot = potRepository.findById(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));

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

    private void syncTags(Til til, List<String> tagNames) {
        til.getTilTags().clear();

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        for (String name : tagNames) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.create(name)));
            til.getTilTags().add(TilTag.of(til, tag));
        }
    }
}
