package com.Rootin.domain.garden.service;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     *
     * [수학적 O(1) 공식 설명 - 동료 개발자 공유용]
     * 원래는 level당 필요한 경험치(1Lv: 100, 2Lv: 200, 3Lv: 300...)를 반복해서 빼주는 O(n) 루프 방식을 사용했습니다.
     * 레벨이 높아질수록 반복 횟수가 많아지는 부하가 있어, 이를 수학적 등차수열의 합과 근의 공식으로 치환하여 O(1)로 연산합니다.
     *
     * 1. n레벨(구간)까지 도달하기 위한 등차수열 기반 최소 누적 경험치 공식:
     *    exp = 50 * n * (n + 1)
     *
     * 2. 양변을 전개하여 n에 대한 2차 방정식 형태로 정렬:
     *    50*n^2 + 50*n - exp = 0  ->  n^2 + n - (exp / 50.0) = 0
     *
     * 3. 2차 방정식 근의 공식 [ x = (-b + sqrt(b^2 - 4ac)) / 2a ] 대입:
     *    n = (-1.0 + sqrt(1.0 - 4.0 * 1.0 * (-totalExp / 50.0))) / 2.0
     *    n = (-1.0 + sqrt(1.0 + 4.0 * totalExp / 50.0)) / 2.0
     *
     * 4. 계산된 n은 레벨 구간 중간의 실수이므로 내림(Math.floor)하여 정수로 바꾼 뒤,
     *    최종 레벨 = n + 1로 반환합니다. (예: totalExp가 150이면 n=1.3 -> 내림 시 1 -> 최종 레벨 2Lv)
     *
     * @param totalExp 현재 화분의 전체 누적 경험치
     * @return 계산된 최종 레벨 (최소 1레벨 보장)
     */
    public int calculateLevel(int totalExp) {
        if (totalExp <= 0) {
            return 1;
        }
        
        // 근의 공식을 활용해 도달한 정수 구간 n을 계산합니다.
        int n = (int) Math.floor((-1.0 + Math.sqrt(1.0 + 4.0 * totalExp / 50.0)) / 2.0);
        
        // n레벨 구간에 1을 더해 현재 실제 레벨로 환산합니다.
        return n + 1;
    }

    /**
     * 식물의 누적 경험치를 기준으로 식물의 5단계 성장 상태(GrowthStage)를 런타임에 판별하여 반환합니다.
     * [성장 단계 기준 테이블]
     * - 0 ~ 199 Exp: SEED (씨앗)
     * - 200 ~ 499 Exp: SPROUT (새싹)
     * - 500 ~ 799 Exp: MATURE (잎)
     * - 800 ~ 999 Exp: BLOOM (개화)
     * - 1000 Exp 이상: FULL_BLOOM (만개 - 수확 가능)
     *
     * @param plantExp 식물의 현재 누적 경험치
     * @return 식물의 현재 성장 단계 (GrowthStage)
     */
    public GrowthStage determinePlantGrowthStage(int plantExp) {
        if (plantExp < 200) {
            return GrowthStage.SEED;
        } else if (plantExp < 500) {
            return GrowthStage.SPROUT;
        } else if (plantExp < 800) {
            return GrowthStage.MATURE;
        } else if (plantExp < 1000) {
            return GrowthStage.BLOOM;
        } else {
            return GrowthStage.FULL_BLOOM;
        }
    }

    /**
     * 식물의 현재 경험치를 바탕으로 전체 만개 기준 성장률(%)을 계산하여 반환합니다.
     * 1000 Exp 도달 시 100.0%를 초과하지 않으며, 소수점 첫째 자리까지 정확하게 반올림합니다.
     *
     * @param growthExp 식물의 현재 누적 경험치
     * @return 0.0 ~ 100.0 사이의 성장도 백분율
     */
    public double calculatePlantGrowthPercentage(int growthExp) {
        int safeExp = Math.max(growthExp, 0);
        return Math.round(Math.min((safeExp / 1000.0) * 100, 100.0) * 10.0) / 10.0;
    }

    /**
     * 식물이 수확 가능한 상태(경험치 1000 이상)인지 여부를 판별합니다.
     * [정책] 식물의 자체적인 성장만을 기준으로 삼기 위해 화분 레벨 조건은 결합하지 않습니다.
     *
     * @param growthExp 식물의 현재 누적 경험치
     * @return 수확 가능 여부
     */
    public boolean canHarvestPlant(int growthExp) {
        return growthExp >= 1000;
    }

    /**
     * 특정 레벨에 도달하기 위해 필요한 누적 최소 경험치(시작점)를 구합니다.
     * [레벨별 누적 최소 경험치 시작점 예시]
     * - 1레벨: 0 Exp
     * - 2레벨: 100 Exp (1레벨에서 2레벨 가는데 100 필요)
     * - 3레벨: 300 Exp (2레벨에서 3레벨 가는데 200 필요 -> 누적 100 + 200 = 300)
     * - 4레벨: 600 Exp (3레벨에서 4레벨 가는데 300 필요 -> 누적 300 + 300 = 600)
     *
     * @param level 대상 레벨
     * @return 해당 레벨이 시작되는 누적 최소 경험치 수치
     */
    public int calculateMinExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        int n = level - 1;
        // 등차수열의 합 공식: 100 * n * (n + 1) / 2 -> 50 * n * (n + 1)
        // 불필요한 O(N) 반복문 루프를 제거하여 O(1) 상수시간 연산으로 성능을 개선했습니다.
        return 50 * n * (n + 1);
    }

    /**
     * 현재 레벨 내에서 순수하게 획득하여 올린 구간 경험치를 계산합니다.
     * [계산 방식]
     * - 전체 누적 경험치(totalExp)에서 현재 레벨이 시작되는 누적 최소 경험치(MinExp)를 뺍니다.
     * - 예시: 누적 경험치 150 Exp, 현재 레벨이 2라면
     *   -> 2레벨 시작점은 100 Exp 이므로, 2레벨 내에서 순수하게 올린 경험치는 150 - 100 = 50 Exp 입니다.
     *
     * @param totalExp 현재 화분의 전체 누적 경험치
     * @param currentLevel 현재 화분의 레벨
     * @return 현재 레벨 구간 내에서 올린 순수 경험치
     */
    public int calculateLevelProgressExp(int totalExp, int currentLevel) {
        int minExpForCurrent = calculateMinExpForLevel(currentLevel);
        int requiredExp = calculateNextLevelRequiredExp(currentLevel);

        // 계산 결과가 음수가 되지 않도록 최소 0을 보장합니다.
        return Math.min(Math.max(0, totalExp - minExpForCurrent), requiredExp);
    }

    /**
     * 다음 레벨로 레벨업을 하기 위해 이 현재 레벨 구간에서 채워야 하는 총 경험치(구간 목표량)를 구합니다.
     * [공식]
     * - 구간 요구량 = 현재 레벨 * 100
     * - 예시: 1레벨일 때 다음 레벨(2Lv)을 가기 위한 구간 요구량은 100 Exp
     * - 예시: 2레벨일 때 다음 레벨(3Lv)을 가기 위한 구간 요구량은 200 Exp
     *
     * @param currentLevel 현재 레벨
     * @return 다음 레벨로 가기 위한 해당 구간의 총 요구 경험치
     */
    public int calculateNextLevelRequiredExp(int currentLevel) {
        if (currentLevel <= 0) {
            return 100; // 0이하의 유효하지 않은 레벨이 들어오면 최소치인 100을 반환합니다.
        }
        return currentLevel * 100;
    }

    /**
     * 현재 레벨 구간에서의 성장 진행률(%)을 소수점 첫째 자리까지 계산하여 반환합니다. (둘째 자리에서 반올림)
     * [계산 공식]
     * - 진행률(%) = (현재 레벨 내에서 올린 경험치 / 다음 레벨로 가기 위한 구간 요구 경험치) * 100.0
     * - 소수점 둘째 자리에서 반올림하여 첫째 자리까지 표현합니다. (예: 50.34% -> 50.3%, 50.35% -> 50.4%)
     *
     * @param totalExp 현재 화분의 전체 누적 경험치
     * @param currentLevel 현재 화분의 레벨
     * @return 소수점 첫째 자리까지 반올림된 진행률 백분율(0.0 ~ 100.0)
     */
    public double calculateProgressPercentage(int totalExp, int currentLevel) {
        int progressExp = calculateLevelProgressExp(totalExp, currentLevel);
        int requiredExp = calculateNextLevelRequiredExp(currentLevel);

        if (requiredExp <= 0) {
            return 0.0;
        }

        // 진행률(%) 실수 계산
        double percentage = ((double) progressExp / requiredExp) * 100.0;

        // 소수점 둘째 자리에서 반올림하여 첫째 자리까지 표현하도록 보정합니다.
        // Math.round(percentage * 10.0)을 수행하면 소수 첫째 자리를 정수 1의 자리로 올린 뒤 반올림하고, 다시 10.0으로 나누어 소수 첫째 자리 형태로 돌려놓습니다.
        return Math.round(percentage * 10.0) / 10.0;
    }

    /**
     * [대시보드 전시용] 유저의 전체 TIL 발행 일자 목록을 기반으로 연속 작성일(스트릭)을 계산합니다.
     * 오늘 작성 완료한 기록이 있다면 오늘 날짜부터 역산하고, 없다면 어제 날짜부터 역산하여
     * 첫 작성 시에도 자연스럽게 "1일 연속 작성"으로 보일 수 있도록 계산합니다.
     *
     * @param publishedTimes 유저가 작성한 TIL들의 발행 시간 목록
     * @return 오늘을 포함해 현재 유지되고 있는 연속 작성일 수 (최소 0)
     */
    public int calculateStreak(List<LocalDateTime> publishedTimes) {
        if (publishedTimes == null || publishedTimes.isEmpty()) {
            return 0;
        }

        return calculateStreakFromDates(publishedTimes.stream()
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .toList());
    }

    /**
     * [대시보드 전시용] 이미 날짜 단위로 축약된 발행 일자 목록을 기반으로 연속 작성일(스트릭)을 계산합니다.
     * 대시보드/화분 상세처럼 많은 요청이 동시에 들어오는 조회 흐름에서는 DB에서 DISTINCT 날짜만 가져와
     * 불필요한 LocalDateTime 객체 생성과 중복 Set 변환 비용을 줄입니다.
     *
     * @param publishedDates 유저가 작성한 TIL들의 발행 날짜 목록
     * @return 오늘을 포함해 현재 유지되고 있는 연속 작성일 수 (최소 0)
     */
    public int calculateStreakFromDates(List<LocalDate> publishedDates) {
        if (publishedDates == null || publishedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        // 날짜 단위 조회를 빠르게 처리하기 위해 Set으로 변환하여 O(1) 검색 속도를 보장합니다.
        Set<LocalDate> dateSet = publishedDates.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 오늘 쓴 TIL이 존재한다면 오늘부터 역산하고, 없다면 어제부터 과거로 역산합니다.
        LocalDate checkDate = dateSet.contains(today) ? today : today.minusDays(1);

        // 어제와 오늘 모두 글을 쓰지 않은 상태라면 스트릭은 0일입니다.
        if (!dateSet.contains(checkDate)) {
            return 0;
        }

        int streak = 0;
        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    /**
     * [경험치 정산용] 유저가 글을 작성하기 "어제까지"의 누적 연속 작성일(스트릭)을 계산합니다.
     * 무조건 어제(today.minusDays(1))부터 과거로 역산하므로, 첫날 처음 TIL 작성 시에는 0일 스트릭(보너스 없음)이 적용됩니다.
     *
     * @param publishedTimes 유저가 작성한 TIL들의 발행 시간 목록
     * @return 어제 기준의 연속 작성일 수 (최소 0)
     */
    public int calculatePreviousStreak(List<LocalDateTime> publishedTimes) {
        if (publishedTimes == null || publishedTimes.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        Set<LocalDate> dateSet = publishedTimes.stream()
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        // 경험치 보너스 판정용이므로 오늘 작성 여부와 무관하게 무조건 어제부터 과거로 역산합니다.
        LocalDate checkDate = today.minusDays(1);

        if (!dateSet.contains(checkDate)) {
            return 0;
        }

        int streak = 0;
        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }
}
