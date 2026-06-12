package com.Rootin.domain.gamification.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트 변동 사유
 * - 퀘스트 계열: 양수 (적립) — 오늘의 목표 달성 시 하루 1회 지급
 * - AI  계열:    음수 (소모) — AI 담당자 구현 완료
 */
@Getter
@RequiredArgsConstructor
public enum PointLogReason {

    // ── 적립 (퀘스트) ────────────────────────────────────────────────────────
    QUEST_Q1("오늘의 목표 Q1 달성: TIL 1개 작성"),
    QUEST_Q2("오늘의 목표 Q2 달성: TIL에 태그 달기"),
    QUEST_Q3("오늘의 목표 Q3 달성: 200자 이상 작성"),

    // ── 적립 (레거시 — 더 이상 사용하지 않음, 기존 DB 레코드 보존용) ──────────
    /** @deprecated 포인트는 오늘의 목표(QUEST_Q*)에서만 지급됩니다. */
    @Deprecated
    TIL_WRITE("TIL 작성"),

    // ── 소모 (AI) ───────────────────────────────────────────────────────────
    AI_SUMMARY("AI 요약 사용"),
    AI_QUIZ("AI 퀴즈 사용");

    private final String description;
}
