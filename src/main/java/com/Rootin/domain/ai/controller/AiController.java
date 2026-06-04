package com.Rootin.domain.ai.controller;

import com.Rootin.domain.ai.dto.AiQuizRequest;
import com.Rootin.domain.ai.dto.AiQuizResponse;
import com.Rootin.domain.ai.dto.AiSummaryRequest;
import com.Rootin.domain.ai.dto.AiSummaryResponse;
import com.Rootin.domain.ai.service.AiService;
import com.Rootin.global.jwt.JwtUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /ai/summary
     * 작성된 TIL을 OpenAI로 요약 — 포인트 {@code SUMMARY_POINT_COST}점 차감
     *
     * @return 200 요약 성공 / 402 포인트 부족 / 403 타인 TIL / 404 TIL 미존재
     */
    @PostMapping("/summary")
    public ResponseEntity<AiSummaryResponse> summary(
            @Valid @RequestBody AiSummaryRequest request,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(aiService.summarize(request, userDetails.getUserId()));
    }

    /**
     * POST /ai/quiz
     * 작성된 TIL을 기반으로 복습 문제 생성 — count × {@code QUIZ_POINT_COST_PER_QUESTION}점 차감
     *
     * @return 200 생성 성공 / 400 count 범위 초과 / 402 포인트 부족 / 403 타인 TIL / 404 TIL 미존재
     */
    @PostMapping("/quiz")
    public ResponseEntity<AiQuizResponse> quiz(
            @Valid @RequestBody AiQuizRequest request,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        return ResponseEntity.ok(aiService.generateQuiz(request, userDetails.getUserId()));
    }
}
