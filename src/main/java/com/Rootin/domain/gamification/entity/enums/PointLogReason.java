package com.Rootin.domain.gamification.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트 변동 사유
 * - TIL 계열: 양수 (적립) → TODO [동기부여 담당자]: TIL 작성 완료 시 적립 로직 구현 필요
 * - AI  계열: 음수 (소모) → AI 담당자 구현 완료
 */
@Getter
@RequiredArgsConstructor
public enum PointLogReason {

    // ── 적립 (TIL) — TODO [동기부여 담당자]: 아래 항목 사용하여 적립 로직 구현 ──
    TIL_WRITE("TIL 작성"),

    // ── 소모 (AI) ───────────────────────────────────────────────────────────
    AI_SUMMARY("AI 요약 사용"),
    AI_QUIZ("AI 퀴즈 사용");

    private final String description;
}
