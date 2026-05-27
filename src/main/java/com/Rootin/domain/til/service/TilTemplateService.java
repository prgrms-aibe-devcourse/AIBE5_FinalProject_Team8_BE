package com.Rootin.domain.til.service;

import com.Rootin.domain.til.dto.request.TilTemplateCreateRequest;
import com.Rootin.domain.til.dto.response.TilTemplateResponse;
import com.Rootin.domain.til.entity.TilTemplate;
import com.Rootin.domain.til.repository.TilTemplateRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TilTemplateService {

    private final TilTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TilTemplateResponse> getTemplates(Long userId) {
        return templateRepository.findByUserIdOrIsDefault(userId).stream()
                .map(TilTemplateResponse::from)
                .toList();
    }

    @Transactional
    public TilTemplateResponse create(Long userId, TilTemplateCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        TilTemplate template = TilTemplate.create(user, request.title(), request.content());
        return TilTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void delete(Long userId, Long templateId) {
        TilTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> CustomException.notFound("템플릿을 찾을 수 없습니다."));

        if (template.isDefault()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "기본 템플릿은 삭제할 수 없습니다.");
        }

        if (!template.getUser().getId().equals(userId)) {
            throw CustomException.forbidden("해당 템플릿에 대한 권한이 없습니다.");
        }

        templateRepository.delete(template);
    }
}
