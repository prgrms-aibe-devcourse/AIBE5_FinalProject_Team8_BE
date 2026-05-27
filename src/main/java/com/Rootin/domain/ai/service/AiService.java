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
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.til.repository.PostRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final int SUMMARY_POINT_COST = AiPolicy.SUMMARY_POINT_COST;
    private static final int QUIZ_POINT_COST_PER_QUESTION = AiPolicy.QUIZ_POINT_COST_PER_QUESTION;

    private final AiPromptClient aiPromptClient;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * TIL 요약 요청
     * 1. 포인트 잔액 확인 (부족 시 402)
     * 2. TIL 조회 + 소유자 검증
     * 3. OpenAI 요약 호출
     * 4. 포인트 차감 + PointLog 저장
     * 5. 응답 반환
     */
    @Transactional
    public AiSummaryResponse summarize(AiSummaryRequest request, User principal) {
        // DB에서 최신 User 상태 조회 (포인트 잔액 신뢰성 보장)
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        // 포인트 잔액 확인
        if (user.getPoint() < SUMMARY_POINT_COST) {
            throw CustomException.paymentRequired("포인트가 부족합니다. (필요: " + SUMMARY_POINT_COST + "P, 보유: " + user.getPoint() + "P)");
        }

        // TIL 조회 및 소유자 검증
        Post post = postRepository.findById(request.tilId())
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(user.getId())) {
            throw CustomException.forbidden("본인의 TIL만 요약할 수 있습니다.");
        }

        // OpenAI 요약 요청
        String responseJson = aiPromptClient.summarizeTil(post.getContent());

        // JSON 파싱
        SummaryResult result = parseSummaryResponse(responseJson);

        // 포인트 차감 및 PointLog 기록
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
     * 3. TIL 조회 + 소유자 검증
     * 4. OpenAI 퀴즈 호출
     * 5. 포인트 차감 + PointLog 저장
     * 6. 응답 반환
     */
    @Transactional
    public AiQuizResponse generateQuiz(AiQuizRequest request, User principal) {
        int totalCost = request.count() * QUIZ_POINT_COST_PER_QUESTION;

        // DB에서 최신 User 상태 조회
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        // 포인트 잔액 확인
        if (user.getPoint() < totalCost) {
            throw CustomException.paymentRequired("포인트가 부족합니다. (필요: " + totalCost + "P, 보유: " + user.getPoint() + "P)");
        }

        // TIL 조회 및 소유자 검증
        Post post = postRepository.findById(request.tilId())
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(user.getId())) {
            throw CustomException.forbidden("본인의 TIL로만 복습 문제를 생성할 수 있습니다.");
        }

        // OpenAI 퀴즈 요청
        String responseJson = aiPromptClient.generateQuiz(post.getContent(), request.count());

        // JSON 파싱
        List<AiQuizItem> quizzes = parseQuizResponse(responseJson);

        // 포인트 차감 및 PointLog 기록
        user.deductPoint(totalCost);
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .reason(PointLogReason.AI_QUIZ)
                .amount(-totalCost)
                .build());

        return new AiQuizResponse(quizzes, totalCost, user.getPoint());
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
