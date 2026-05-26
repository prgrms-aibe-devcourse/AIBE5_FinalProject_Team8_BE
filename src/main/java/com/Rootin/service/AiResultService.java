package com.Rootin.service;

import com.Rootin.domain.AiResult;
import com.Rootin.domain.Post;
import com.Rootin.domain.User;
import com.Rootin.domain.enums.ToolType;
import com.Rootin.dto.AiResultResponse;
import com.Rootin.dto.AiResultSaveRequest;
import com.Rootin.exception.CustomException;
import com.Rootin.repository.AiResultRepository;
import com.Rootin.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiResultService {

    private final AiResultRepository aiResultRepository;
    private final PostRepository postRepository;

    @Transactional
    public AiResultResponse save(AiResultSaveRequest request, User currentUser) {
        // QUIZ 타입 전용 필드 검증
        if (request.type() == ToolType.QUIZ) {
            if (request.difficulty() == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "QUIZ 타입은 difficulty가 필수입니다.");
            }
            if (request.count() == null || request.count() < 1) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "QUIZ 타입은 count가 1 이상이어야 합니다.");
            }
        }

        Post post = postRepository.findById(request.tilId())
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        // 본인 TIL인지 검증
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw CustomException.forbidden("본인의 TIL에만 AI 결과를 저장할 수 있습니다.");
        }

        AiResult aiResult = AiResult.builder()
                .post(post)
                .user(currentUser)
                .resultContent(request.content())
                .toolType(request.type())
                .count(request.count())
                .difficulty(request.difficulty())
                .build();

        return AiResultResponse.from(aiResultRepository.save(aiResult));
    }
}
