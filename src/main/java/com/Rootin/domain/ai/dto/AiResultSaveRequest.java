package com.Rootin.domain.ai.dto;

import com.Rootin.domain.ai.entity.enums.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiResultSaveRequest(

        @NotNull(message = "toolType은 필수입니다.")
        ToolType type,

        @NotNull(message = "potId는 필수입니다.")
        Long potId,

        @NotBlank(message = "content는 필수입니다.")
        String content
) {}
