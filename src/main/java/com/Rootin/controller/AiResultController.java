package com.Rootin.controller;

import com.Rootin.domain.User;
import com.Rootin.dto.AiResultResponse;
import com.Rootin.dto.AiResultSaveRequest;
import com.Rootin.service.AiResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/results")
@RequiredArgsConstructor
public class AiResultController {

    private final AiResultService aiResultService;

    /**
     * POST /ai/results
     *
     * TODO [로그인 담당자]: @AuthenticationPrincipal로 User 엔티티가 주입되려면
     * JwtAuthenticationFilter에서 SecurityContextHolder에 UsernamePasswordAuthenticationToken을 설정할 때
     * principal로 User 엔티티(또는 UserDetails 구현체)를 넣어줘야 합니다.
     * ex) new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
     */
    @PostMapping
    public ResponseEntity<AiResultResponse> save(
            @Valid @RequestBody AiResultSaveRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(aiResultService.save(request, currentUser));
    }
}
