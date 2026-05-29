package com.Rootin.domain.ai.dto;

import java.util.List;

/** 퀴즈 단건 — question / choices / answer / hint */
public record AiQuizItem(
        String question,
        List<String> choices,
        String answer,
        String hint
) {}
