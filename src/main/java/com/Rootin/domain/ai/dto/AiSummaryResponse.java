package com.Rootin.domain.ai.dto;

import java.util.List;

public record AiSummaryResponse(

        /** AI가 생성한 TIL 핵심 요약 (2~3문장) */
        String summary,

        /** AI가 추출한 핵심 포인트 목록 (최대 5개) */
        List<String> keyPoints,

        /** 이번 요약에 사용된 포인트 */
        int usedPoint,

        /** 차감 후 남은 포인트 잔액 */
        int remainPoint
) {}
