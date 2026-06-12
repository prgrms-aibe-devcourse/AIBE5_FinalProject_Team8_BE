package com.Rootin.domain.ai.constant;

/**
 * AI 기능 관련 정책 상수
 *
 * 사용 모델: GPT-5.4 nano (Input $0.20/1M tokens, Output $1.25/1M tokens)
 *
 * 실측 기준 (테스트 계정의 코딩 화분 TIL 173개 전체 요약 1회당 토큰 및 비용):
 *   - Input  ~100,000 tokens → $0.020 (≈ 30원) 수준
 *   - Output ~  1,000 tokens → $0.001 (≈  1원) 수준
 *   - 1회 실제 API 비용 ≈ 30원
 *
 * 포인트 정책 (1P = 1원 환산 기준):
 *   - 요약 1회: 50P → 사용자 부담 50원, API 원가 30원 → 마진 약 40%
 *   - 퀴즈 10문항: 100P → 사용자 부담 100원, API 원가 ~50원 → 마진 약 50%
 *
 * 무료 제공 포인트로 사용자가 하루 1~2회 이용 시 서비스가 비용을 부담하지만,
 * 결제를 추가하고 이를 통해 포인트를 충전하는 유저가 전체의 10~15%만 넘으면 손익분기를 초과한다.
 * 즉, 무료 유저는 마케팅 비용, 결제 유저는 수익원으로 작동하는 프리미엄 구조로 설계되었다.
 *
 * TODO: 모델을 mini/5.4로 업그레이드 시 비용이 3~10배 증가하므로 포인트 정책 재검토 필요
 */
public final class AiPolicy {

    /** 요약 1회 차감 포인트*/
    public static final int SUMMARY_POINT_COST = 50;

    /** 퀴즈 1문항당 포인트 비용 (총 비용 = count × QUIZ_POINT_COST_PER_QUESTION) */
    public static final int QUIZ_POINT_COST_PER_QUESTION = 10;

    /** 퀴즈 최대 문항 수 */
    public static final int QUIZ_MAX_COUNT = 10;

    /**
     * tilIds 선택 시 한 번에 전달 가능한 최대 TIL 개수.
     * 실측 기준 TIL 173개 전체 전송이 input ~100K 토큰으로 API 비용 약 30원 수준이므로
     * 토큰 비용 제한이 아닌 단일 요청의 합리적 상한선으로 200개를 설정한다.
     * (대학 시험이 책 한 권을 통째로 범위에 넣듯, 화분 전체를 AI에 넘기는 유즈케이스를 허용하려 합니다)
     */
    public static final int TIL_IDS_MAX_SIZE = 200;

    /** 상수 클래스 — 인스턴스화 방지 */
    private AiPolicy() {}
}
