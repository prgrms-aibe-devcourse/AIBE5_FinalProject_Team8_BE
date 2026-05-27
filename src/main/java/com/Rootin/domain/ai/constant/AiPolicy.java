package com.Rootin.domain.ai.constant;

/**
 * AI 기능 관련 정책 상수
 * TODO: 포인트 비용 정책 변경 시 이 파일만 수정하면 됩니다
 */
public final class AiPolicy {

    public static final int SUMMARY_POINT_COST = 50;

    /** 퀴즈 1문항당 포인트 비용 (총 비용 = count × QUIZ_POINT_COST_PER_QUESTION) */
    public static final int QUIZ_POINT_COST_PER_QUESTION = 10;

    /** 퀴즈 최대 문항 수 */
    public static final int QUIZ_MAX_COUNT = 10;

    /** 상수 클래스 — 인스턴스화 방지 */
    private AiPolicy() {}
}
