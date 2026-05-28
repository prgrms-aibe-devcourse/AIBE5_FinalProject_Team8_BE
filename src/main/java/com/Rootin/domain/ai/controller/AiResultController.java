package com.Rootin.domain.ai.controller;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.ai.dto.AiResultResponse;
import com.Rootin.domain.ai.dto.AiResultSaveRequest;
import com.Rootin.domain.ai.service.AiResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/results")
@RequiredArgsConstructor
public class AiResultController {

    private final AiResultService aiResultService;

    /**
     * POST /ai/results
     * TODO : JWT 필터에서 principal로 User 엔티티 주입 필요
     */
    @PostMapping
    public ResponseEntity<AiResultResponse> save(
            @Valid @RequestBody AiResultSaveRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(aiResultService.save(request, currentUser));
    }

    /**
     * GET /ai/results           → 본인 전체 AI 결과 목록
     * GET /ai/results?potId=1   → 특정 화분 기준 필터링 (타인 화분 시 403)
     */
    @GetMapping
    public ResponseEntity<List<AiResultResponse>> getResults(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long potId
    ) {
        return ResponseEntity.ok(aiResultService.getResults(currentUser, potId));
    }

    /**
     * DELETE /ai/results/{resultId}
     * 본인 결과만 삭제 가능 (타인 결과 시 403, 없는 resultId 시 404)
     */
    @DeleteMapping("/{resultId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long resultId,
            @AuthenticationPrincipal User currentUser
    ) {
        aiResultService.delete(resultId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
