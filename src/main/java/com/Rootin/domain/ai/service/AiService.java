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
     * 1. 포인트 잔액 확인 (부족 시 402)
     * 2. 화분 조회 + 소유자 검증
     * 3. 화분 내 TIL 목록 조회 (게시된 것만) — 없으면 404
     * 4. TIL 내용 합산 → OpenAI 요약 호출
     * 5. 포인트 차감 + PointLog 저장
     * 6. 응답 반환
     */
    @Transactional
    public AiSummaryResponse summarize(AiSummaryRequest request, User principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (user.getPoint() < SUMMARY_POINT_COST) {
            throw CustomException.paymentRequired(
                    "포인트가 부족합니다. (필요: " + SUMMARY_POINT_COST + "P, 보유: " + user.getPoint() + "P)");
        }

        Pot pot = potRepository.findById(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));

        if (!pot.getUserId().equals(user.getId())) {
            throw CustomException.forbidden("본인의 화분만 요약할 수 있습니다.");
        }

        List<Til> tils = tilRepository.findByUserIdAndPotIdAndStatus(
                user.getId(), request.potId(), PostStatus.PUBLISHED);

        if (tils.isEmpty()) {
            throw CustomException.notFound("요약할 TIL이 없습니다.");
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
     * 1. 총 비용 계산 (count × 문항당 비용)
     * 2. 포인트 잔액 확인 (부족 시 402)
     * 3. 화분 조회 + 소유자 검증
     * 4. 화분 내 TIL 목록 조회 — 없으면 404
     * 5. TIL 내용 합산 → OpenAI 퀴즈 호출
     * 6. 포인트 차감 + PointLog 저장
     * 7. 응답 반환
     */
    @Transactional
    public AiQuizResponse generateQuiz(AiQuizRequest request, User principal) {
        int totalCost = request.count() * QUIZ_POINT_COST_PER_QUESTION;

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (user.getPoint() < totalCost) {
            throw CustomException.paymentRequired(
                    "포인트가 부족합니다. (필요: " + totalCost + "P, 보유: " + user.getPoint() + "P)");
        }

        Pot pot = potRepository.findById(request.potId())
                .orElseThrow(() -> CustomException.notFound("화분을 찾을 수 없습니다."));

        if (!pot.getUserId().equals(user.getId())) {
            throw CustomException.forbidden("본인의 화분으로만 복습 문제를 생성할 수 있습니다.");
        }

        List<Til> tils = tilRepository.findByUserIdAndPotIdAndStatus(
                user.getId(), request.potId(), PostStatus.PUBLISHED);

        if (tils.isEmpty()) {
            throw CustomException.notFound("문제를 생성할 TIL이 없습니다.");
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

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────

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
