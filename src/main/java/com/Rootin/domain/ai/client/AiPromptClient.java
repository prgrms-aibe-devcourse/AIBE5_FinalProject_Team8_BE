package com.Rootin.domain.ai.client;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI API 호출을 캡슐화한 클라이언트
 * - AiService에서 직접 SDK를 다루지 않도록 분리 → 테스트 시 이 빈만 Mock으로 교체
 * - openai-java 2.x SDK 기반으로 작성 (com.openai:openai-java:2.1.0)
 */
@Component
@RequiredArgsConstructor
public class AiPromptClient {

    private final OpenAIClient openAIClient;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.summary.max-tokens}")
    private int summaryMaxTokens;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            당신은 학습 도우미입니다.
            주어진 TIL(Today I Learned) 내용을 분석하여 한국어로 응답하세요.
            반드시 아래 형식의 유효한 JSON만 반환하세요 (설명 없이 JSON만):
            {
              "summary": "핵심 내용을 2~3문장으로 요약한 텍스트",
              "keyPoints": ["핵심 포인트1", "핵심 포인트2", "핵심 포인트3"]
            }
            keyPoints는 최대 5개까지 작성하세요.
            """;

    /**
     * TIL 본문을 OpenAI API에 전달해 요약 JSON 문자열을 반환한다.
     *
     * @param tilContent TIL 본문
     * @return AI 응답 JSON 문자열 (summary, keyPoints 포함)
     */
    public String summarizeTil(String tilContent) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens((long) summaryMaxTokens)
                .addSystemMessage(SUMMARY_SYSTEM_PROMPT)
                .addUserMessage(tilContent)
                .build();

        ChatCompletion completion = openAIClient.chat().completions().create(params);

        return completion.choices().get(0).message().content()
                .orElseThrow(() -> new RuntimeException("OpenAI 응답이 비어있습니다."));
    }
}
