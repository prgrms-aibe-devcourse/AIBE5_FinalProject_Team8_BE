package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.dto.AiResultResponse;
import com.Rootin.domain.ai.dto.AiResultSaveRequest;
import com.Rootin.domain.ai.repository.AiResultRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiResultService {

    private final AiResultRepository aiResultRepository;
    private final PotRepository      potRepository;
    private final TilRepository      tilRepository;
    private final UserRepository     userRepository;

    /**
     * AI 결과 저장
     * 1. 화분 조회 + 소유자 검증
     * 2. 해당 화분의 TIL 목록 조회 (게시된 것만)
     * 3. TIL이 없으면 404
     * 4. AiResult 저장 후 AiResultTil 연결
     */
    @Transactional
    public AiResultResponse save(AiResultSaveRequest request, Long userId) {
        Pot pot = potRepository.findById(request.potId())
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }

        List<Til> tils = tilRepository.findByUserIdAndPotIdAndStatus(
                userId, request.potId(), PostStatus.PUBLISHED);

        if (tils.isEmpty()) {
            throw CustomException.of(ErrorCode.TIL_NOT_FOUND);
        }

        // JPA FK 설정을 위해 프록시 참조 사용 (실제 SELECT 없이 ID만 사용)
        User userRef = userRepository.getReferenceById(userId);

        AiResult aiResult = AiResult.builder()
                .user(userRef)
                .resultContent(request.content())
                .toolType(request.type())
                .build();

        AiResult saved = aiResultRepository.save(aiResult);
        tils.forEach(saved::addTil);

        return AiResultResponse.of(saved, request.potId());
    }

    /**
     * AI 결과 목록 조회
     * potId 없음 → 본인 전체 결과
     * potId 있음 → 해당 화분 기준 필터링 (소유자 검증 포함)
     */
    @Transactional(readOnly = true)
    public List<AiResultResponse> getResults(Long userId, Long potId) {
        User userRef = userRepository.getReferenceById(userId);

        if (potId == null) {
            return aiResultRepository.findAllByUser(userRef).stream()
                    .map(ar -> AiResultResponse.of(ar, resolvePotId(ar)))
                    .toList();
        }

        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }

        return aiResultRepository.findAllByUserAndPotId(userRef, potId).stream()
                .map(ar -> AiResultResponse.of(ar, potId))
                .toList();
    }

    @Transactional
    public void delete(Long resultId, Long userId) {
        AiResult aiResult = aiResultRepository.findById(resultId)
                .orElseThrow(() -> CustomException.of(ErrorCode.AI_RESULT_NOT_FOUND));

        if (!aiResult.getUser().getId().equals(userId)) {
            throw CustomException.forbidden("본인의 AI 결과만 삭제할 수 있습니다.");
        }

        aiResultRepository.delete(aiResult);
    }

    /**
     * 전체 조회 시 potId 복원
     * 첫 번째 TIL의 pot.id 반환 (모든 TIL은 동일 화분 소속)
     */
    private Long resolvePotId(AiResult aiResult) {
        return aiResult.getTils().stream()
                .map(til -> til.getPot().getId())
                .findFirst()
                .orElse(null);
    }
}
