package com.Rootin.domain.garden.service;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.springframework.stereotype.Component;

/**
 * 화분과 식물의 경험치 획득량, 레벨업 여부, 포인트 지급량, 그리고 성장 단계를 수학적으로 계산해주는 전담 계산 유틸리티 컴포넌트입니다.
 */
@Component
public class LevelCalculator {

    /**
     * TIL 본문 글자 수와 연속 작성일(스트릭)을 기반으로 최종 경험치를 산출합니다.
     * [공식]
     * - 기본 경험치 = min(글자 수 * 0.2, 300) (글자 수 최대 1,500자까지 인정)
     * - 스트릭 가중치 = 1.0 + min(연속 작성일 * 0.05, 0.5) (매일 5% 보너스, 최대 50% 보너스)
     * - 최종 경험치 = floor(기본 경험치 * 스트릭 가중치)
     *
     * @param contentLength TIL 본문의 글자 수
     * @param streakDays    어제까지의 연속 작성일 수 (당일 미포함 또는 이전까지의 스트릭)
     * @return 계산된 최종 정수형 경험치
     */
    public int calculateExperience(int contentLength, int streakDays) {
        if (contentLength <= 0) {
            return 0;
        }
        
        // 1. 기본 경험치 계산 (글자당 0.2점, 상한선 300점)
        double baseExp = Math.min(contentLength * 0.2, 300.0);
        
        // 2. 스트릭 보너스 가중치 계산 (연속 작성일당 +5% 보너스, 상한선 +50% 보너스)
        double streakWeight = calculateStreakMultiplier(streakDays);
        
        // 3. 최종 경험치는 부동소수점 연산 오차(예: 100 * 1.15가 114.9999... 로 연산되는 문제) 방지를 위해
        //    미세값(1e-9)을 더해준 뒤 소수점 이하 버림(Floor) 처리하여 정수로 반환합니다.
        return (int) Math.floor((baseExp * streakWeight) + 1e-9);
    }

    /**
     * 연속 작성일(스트릭)을 기반으로 경험치 보너스 배율을 계산합니다.
     * [공식]
     * - 스트릭 가중치 배율 = 1.0 + min(연속 작성일 * 0.05, 0.5) (매일 5% 보너스, 최대 50% 보너스)
     *
     * @param streakDays 어제까지의 연속 작성일 수
     * @return 계산된 최종 배율 실수값 (최소 1.0, 최대 1.5)
     */
    public double calculateStreakMultiplier(int streakDays) {
        return 1.0 + Math.min(streakDays * 0.05, 0.5);
    }

    /**
     * 획득한 최종 경험치를 기준으로 사용자에게 지급할 포인트를 계산합니다.
     * [공식]
     * - 획득 포인트 = floor(최종 획득 경험치 * 0.1) (경험치 획득량의 10% 지급)
     *
     * @param finalExp 획득한 최종 경험치량
     * @return 사용자에게 가산할 포인트
     */
    public int calculatePoints(int finalExp) {
        if (finalExp <= 0) {
            return 0;
        }
        // 최종 획득 경험치의 10%를 포인트로 정적 환산하므로, 부동소수점 오차가 발생하지 않도록
        // 10으로 나눈 몫(정수 나눗셈)을 직접 반환합니다.
        return finalExp / 10;
    }

    /**
     * 누적 경험치(totalExp)를 기반으로 현재 화분의 최종 레벨을 산출합니다.
     * [레벨업 정책]
     * - 다음 레벨에 도달하기 위해 필요한 경험치 = 현재 레벨 * 100
     * - 예) 1Lv -> 2Lv: 100 Exp 필요 (누적 100)
     * - 예) 2Lv -> 3Lv: 200 Exp 필요 (누적 300)
     * - 예) 3Lv -> 4Lv: 300 Exp 필요 (누적 600)
     *
     * @param totalExp 현재 화분의 전체 누적 경험치
     * @return 계산된 최종 레벨 (최소 1레벨 보장)
     */
    public int calculateLevel(int totalExp) {
        if (totalExp < 0) {
            return 1;
        }
        
        int level = 1;
        int remainingExp = totalExp;
        
        while (true) {
            int nextLevelExpRequired = level * 100;
            if (remainingExp >= nextLevelExpRequired) {
                remainingExp -= nextLevelExpRequired;
                level++;
            } else {
                break;
            }
        }
        
        return level;
    }

    /**
     * 화분의 레벨을 기준으로 식물의 5단계 성장 상태(GrowthStage)를 런타임에 판별하여 반환합니다.
     * [성장 단계 기준 테이블]
     * - 1Lv ~ 2Lv: SEED (씨앗)
     * - 3Lv ~ 5Lv: SPROUT (새싹)
     * - 6Lv ~ 9Lv: MATURE (잎)
     * - 10Lv ~ 14Lv: BLOOM (개화)
     * - 15Lv 이상: FULL_BLOOM (만개 - 수확 가능)
     *
     * @param potLevel 현재 화분의 레벨
     * @return 식물의 현재 성장 단계 (GrowthStage)
     */
    public GrowthStage determineGrowthStage(int potLevel) {
        if (potLevel <= 2) {
            return GrowthStage.SEED;
        } else if (potLevel <= 5) {
            return GrowthStage.SPROUT;
        } else if (potLevel <= 9) {
            return GrowthStage.MATURE;
        } else if (potLevel <= 14) {
            return GrowthStage.BLOOM;
        } else {
            return GrowthStage.FULL_BLOOM;
        }
    }
}
