package com.Rootin.domain.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuizItemTest {

    @Test
    @DisplayName("AiQuizItem — 모든 필드가 정상적으로 저장된다")
    void createAiQuizItem_allFieldsStored() {
        List<String> choices = List.of("정답", "오답1", "오답2", "오답3");

        AiQuizItem item = new AiQuizItem("문제", choices, "정답", "힌트");

        assertThat(item.question()).isEqualTo("문제");
        assertThat(item.choices()).containsExactly("정답", "오답1", "오답2", "오답3");
        assertThat(item.answer()).isEqualTo("정답");
        assertThat(item.hint()).isEqualTo("힌트");
    }

    @Test
    @DisplayName("AiQuizItem — choices는 정확히 4개여야 한다")
    void createAiQuizItem_choicesHasFourElements() {
        List<String> choices = List.of("정답", "오답1", "오답2", "오답3");

        AiQuizItem item = new AiQuizItem("문제", choices, "정답", "힌트");

        assertThat(item.choices()).hasSize(4);
    }

    @Test
    @DisplayName("AiQuizItem — answer는 choices 중 하나와 일치한다")
    void createAiQuizItem_answerIsOneOfChoices() {
        List<String> choices = List.of("정답", "오답1", "오답2", "오답3");

        AiQuizItem item = new AiQuizItem("문제", choices, "정답", "힌트");

        assertThat(item.choices()).contains(item.answer());
    }
}
