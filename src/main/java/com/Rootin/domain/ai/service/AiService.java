package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.client.AiPromptClient;
import com.Rootin.domain.ai.constant.AiPolicy;
import com.Rootin.domain.ai.dto.AiQuizItem;
import com.Rootin.domain.ai.dto.AiQuizRequest;
import com.Rootin.domain.ai.dto.AiQuizResponse;
import com.Rootin.domain.ai.dto.AiSummaryRequest;
import com.Rootin.domain.ai.dto.AiSummaryResponse;
import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final int SUMMARY_POINT_COST = AiPolicy.SUMMARY_POINT_COST;
    private static final int QUIZ_POINT_COST_PER_QUESTION = AiPolicy.QUIZ_POINT_COST_PER_QUESTION;

    private final AiPromptClient aiPromptClient;
    private final PotRepository potRepository;
    private final TilRepository tilRepository;
    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * TIL 요약 요청
     * [tilIds 없음] 기존 로직: 화분 소유 검증 -> 화분 내 전체 PUBLISHED TIL 사용 (하위 호환)
     * [tilIds 있음] 선택 로직: tilIds로 TIL 조회 -> 소유 검증(400) -> 빈 결과(404)
     * 공통: 포인트 확인(402) -> 합산 -> OpenAI -> 포인트 차감 + PointLog
     */
    @Transactional
    public AiSummaryResponse summarize(AiSummaryRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (user.getPoint() < SUMMARY_POINT_COST) {
            throw CustomException.paymentRequired(
                    "포인트가 부족합니다. (필요: " + SUMMARY_POINT_COST + "P, 보유: " + user.getPoint() + "P)");
        }

        List<Til> tils;
        if (request.tilIds() != null && !request.tilIds().isEmpty()) {
            tils = resolveTilsByIds(request.tilIds(), userId, "요약");
        } else {
            Pot pot = potRepository.findById(request.potId())
                    .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));
            if (!pot.getUserId().equals(user.getId())) {
                throw CustomException.forbidden("본인의 화분만 요약할 수 있습니다.");
            }
            tils = tilRepository.findByUserIdAndPotIdAndStatus(
                    user.getId(), request.potId(), PostStatus.PUBLISHED);
            if (tils.isEmpty()) {
                throw CustomException.notFound("요약할 TIL이 없습니다.");
            }
        }

        String combinedContent = combineContents(tils);
        String responseJson = aiPromptClient.summarizeTil(combinedContent);
        SummaryResult result = parseSummaryResponse(responseJson);

        user.deductPoint(SUMMARY_POINT_COST);
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .reason(PointLogReason.AI_SUMMARY)
                .amount(-SUMMARY_POINT_COST)
                .build());

        return new AiSummaryResponse(
                result.summary(),
                result.keyPoints(),
                SUMMARY_POINT_COST,
                user.getPoint()
        );
    }

    /**
     * 복습 문제 생성 요청
     * [tilIds 없음] 기존 로직: 화분 소유 검증 -> 화분 내 전체 PUBLISHED TIL 사용 (하위 호환)
     * [tilIds 있음] 선택 로직: tilIds로 TIL 조회 -> 소유 검증(400) -> 빈 결과(404)
     * 공통: 포인트 확인(402) -> 합산 -> OpenAI -> 포인트 차감 + PointLog
     */
    @Transactional
    public AiQuizResponse generateQuiz(AiQuizRequest request, Long userId) {
        int totalCost = request.count() * QUIZ_POINT_COST_PER_QUESTION;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (user.getPoint() < totalCost) {
            throw CustomException.paymentRequired(
                    "포인트가 부족합니다. (필요: " + totalCost + "P, 보유: " + user.getPoint() + "P)");
        }

        List<Til> tils;
        if (request.tilIds() != null && !request.tilIds().isEmpty()) {
            tils = resolveTilsByIds(request.tilIds(), userId, "퀴즈 생성");
        } else {
            Pot pot = potRepository.findById(request.potId())
                    .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));
            if (!pot.getUserId().equals(user.getId())) {
                throw CustomException.forbidden("본인의 화분으로만 복습 문제를 생성할 수 있습니다.");
            }
            tils = tilRepository.findByUserIdAndPotIdAndStatus(
                    user.getId(), request.potId(), PostStatus.PUBLISHED);
            if (tils.isEmpty()) {
                throw CustomException.notFound("문제를 생성할 TIL이 없습니다.");
            }
        }

        String combinedContent = combineContents(tils);
        String responseJson = aiPromptClient.generateQuiz(combinedContent, request.count());
        List<AiQuizItem> quizzes = parseQuizResponse(responseJson);

        user.deductPoint(totalCost);
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .reason(PointLogReason.AI_QUIZ)
                .amount(-totalCost)
                .build());

        return new AiQuizResponse(quizzes, totalCost, user.getPoint());
    }

    // ----------------------------------------------------------------

    /**
     * tilIds로 TIL을 조회하고 유효성을 검증한다.
     * - userId 조건을 DB 레벨에서 적용해 타인의 LONGTEXT가 메모리에 올라오지 않도록 차단
     * - 조회 결과가 비어 있으면 404
     * - 요청한 개수와 조회된 개수가 다르면 400 (존재하지 않거나 타인 소유 TIL 포함)
     */
    private List<Til> resolveTilsByIds(List<Long> tilIds, Long userId, String action) {
        List<Til> tils = tilRepository.findAllByIdInAndStatusAndUserId(tilIds, PostStatus.PUBLISHED, userId);
        if (tils.isEmpty()) {
            throw CustomException.notFound(action + "할 TIL이 없습니다.");
        }
        if (tils.size() != tilIds.size()) {
            throw CustomException.badRequest("존재하지 않거나 접근할 수 없는 TIL이 포함되어 있습니다.");
        }
        return tils;
    }

    /** 여러 TIL 내용을 구분자로 합산하여 하나의 문자열로 반환 */
    private String combineContents(List<Til> tils) {
        return tils.stream()
                .map(Til::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private SummaryResult parseSummaryResponse(String json) {
        try {
            return objectMapper.readValue(json, SummaryResult.class);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱에 실패했습니다.");
        }
    }

    private List<AiQuizItem> parseQuizResponse(String json) {
        try {
            QuizResult result = objectMapper.readValue(json, QuizResult.class);
            return result.quizzes();
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱에 실패했습니다.");
        }
    }

    /** OpenAI 요약 응답 JSON 구조 */
    private record SummaryResult(String summary, List<String> keyPoints) {}

    /** OpenAI 퀴즈 응답 JSON 구조 */
    private record QuizResult(List<AiQuizItem> quizzes) {}
}
