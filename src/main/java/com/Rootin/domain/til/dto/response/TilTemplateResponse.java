package com.Rootin.domain.til.dto.response;

import com.Rootin.domain.til.entity.TilTemplate;

public record TilTemplateResponse(
        Long templateId,
        String title,
        String content,
        boolean isDefault
) {
    public static TilTemplateResponse from(TilTemplate template) {
        return new TilTemplateResponse(
                template.getId(),
                template.getTitle(),
                template.getContent(),
                template.isDefault()
        );
    }
}
