package com.Rootin.domain.ai.dto;

import com.Rootin.domain.ai.entity.enums.Difficulty;
import com.Rootin.domain.ai.entity.enums.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiResultSaveRequest(

        @NotNull(message = "toolType은 필수입니다.")
        ToolType type,

        @NotNull(message = "tilId는 필수입니다.")
        Long tilId,

        @NotBlank(message = "content는 필수입니다.")
        String content,

        // QUIZ일 때만 필수 - 서비스 레이어에서 검증
        Difficulty difficulty,

        // QUIZ일 때만 필수 (1 이상) - 서비스 레이어에서 검증, SUMMARY는 null 허용
        Integer count
) {}
