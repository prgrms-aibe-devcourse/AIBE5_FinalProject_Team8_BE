package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.ai.dto.AiResultResponse;
import com.Rootin.domain.ai.dto.AiResultSaveRequest;
import com.Rootin.domain.ai.repository.AiResultRepository;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.til.repository.PostRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiResultService {

    private final AiResultRepository aiResultRepository;
    private final PostRepository postRepository;

    @Transactional
    public AiResultResponse save(AiResultSaveRequest request, User currentUser) {
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

    @Transactional(readOnly = true)
    public List<AiResultResponse> getResults(User currentUser, Long tilId) {
        if (tilId == null) {
            return aiResultRepository.findAllByUser(currentUser).stream()
                    .map(AiResultResponse::from)
                    .toList();
        }

        Post post = postRepository.findById(tilId)
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw CustomException.forbidden("본인의 TIL 결과만 조회할 수 있습니다.");
        }

        return aiResultRepository.findAllByUserAndPost(currentUser, post).stream()
                .map(AiResultResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long resultId, User currentUser) {
        AiResult aiResult = aiResultRepository.findById(resultId)
                .orElseThrow(() -> CustomException.notFound("AI 결과를 찾을 수 없습니다."));

        if (!aiResult.getUser().getId().equals(currentUser.getId())) {
            throw CustomException.forbidden("본인의 AI 결과만 삭제할 수 있습니다.");
        }

        aiResultRepository.delete(aiResult);
    }
}
