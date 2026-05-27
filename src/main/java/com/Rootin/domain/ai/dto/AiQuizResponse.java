package com.Rootin.domain.ai.dto;

import java.util.List;

public record AiQuizResponse(

        /** 생성된 퀴즈 목록 */
        List<AiQuizItem> quizzes,

        /** 이번 퀴즈 생성에 사용된 포인트 (count × 문항당 비용) */
        int usedPoint,

        /** 차감 후 남은 포인트 잔액 */
        int remainPoint
) {}
