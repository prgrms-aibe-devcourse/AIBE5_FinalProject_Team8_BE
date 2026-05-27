package com.Rootin.domain.ai.dto;

/** 퀴즈 단건 — question / answer / hint */
public record AiQuizItem(
        String question,
        String answer,
        String hint
) {}
