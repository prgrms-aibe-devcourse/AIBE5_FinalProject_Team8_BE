package com.Rootin.domain.garden.service;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LevelCalculatorTest {

    private final LevelCalculator calculator = new LevelCalculator();

    @Nested
    @DisplayName("경험치 계산 공식 검증")
    class ExperienceCalculationTest {

        @Test
        @DisplayName("일반적인 글자 수와 스트릭 0일 때, 글자 수 * 0.2의 값이 정수로 반환된다")
        void calculateExpNormalNoStreak() {
            // given
            int contentLength = 500;
            int streakDays = 0;

            // when
            int result = calculator.calculateExperience(contentLength, streakDays);

            // then
            // 500 * 0.2 = 100. 가중치 1.0 => 100
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("글자 수에 따른 기본 경험치는 최대 300점으로 제한된다 (1,500자 초과 시 300점 고정)")
        void calculateExpWithMaxCap() {
            // given
            int contentLength = 2000; // 2000 * 0.2 = 400 이지만 최대 300점
            int streakDays = 0;

            // when
            int result = calculator.calculateExperience(contentLength, streakDays);

            // then
            assertThat(result).isEqualTo(300);
        }

        @Test
        @DisplayName("스트릭(연속 작성일)이 존재하면 하루당 5%씩 가중치가 더해진다")
        void calculateExpWithStreak() {
            // given
            int contentLength = 500; // 기본 100점
            int streakDays = 5;      // 보너스 25% (1.25 배율)

            // when
            int result = calculator.calculateExperience(contentLength, streakDays);

            // then
            // 100 * 1.25 = 125
            assertThat(result).isEqualTo(125);
        }

        @Test
        @DisplayName("스트릭 보너스 가중치는 최대 50% (+0.5)로 제한된다 (10일 초과 시 1.5배 고정)")
        void calculateExpWithMaxStreakCap() {
            // given
            int contentLength = 500; // 기본 100점
            int streakDays = 15;     // 보너스 75%가 아닌 최대 50% 제한 (1.5 배율)

            // when
            int result = calculator.calculateExperience(contentLength, streakDays);

            // then
            // 100 * 1.5 = 150
            assertThat(result).isEqualTo(150);
        }

        @Test
        @DisplayName("계산 결과 중 소수점 이하는 버림(Floor) 처리된다")
        void calculateExpWithFloor() {
            // given
            int contentLength = 103; // 기본 경험치: 103 * 0.2 = 20.6
            int streakDays = 1;      // 보너스 5% (1.05 배율) -> 20.6 * 1.05 = 21.63

            // when
            int result = calculator.calculateExperience(contentLength, streakDays);

            // then
            // 21.63 => floor 버림 처리하여 21
            assertThat(result).isEqualTo(21);
        }

        @Test
        @DisplayName("글자 수가 0 이하이면 0점을 반환한다")
        void calculateExpWithZeroOrNegativeLength() {
            assertThat(calculator.calculateExperience(0, 5)).isEqualTo(0);
            assertThat(calculator.calculateExperience(-10, 5)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("포인트 계산 공식 검증")
    class PointCalculationTest {

        @Test
        @DisplayName("포인트는 경험치 획득량의 10%이며, 소수점은 버려진다")
        void calculatePointsSuccess() {
            assertThat(calculator.calculatePoints(200)).isEqualTo(20);
            assertThat(calculator.calculatePoints(155)).isEqualTo(15);
            assertThat(calculator.calculatePoints(0)).isEqualTo(0);
            assertThat(calculator.calculatePoints(-10)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("화분 레벨 계산 공식 검증")
    class LevelCalculationTest {

        @Test
        @DisplayName("누적 경험치가 부족하거나 0 이하이면 1레벨이다")
        void calculateLevelMin() {
            assertThat(calculator.calculateLevel(-50)).isEqualTo(1);
            assertThat(calculator.calculateLevel(0)).isEqualTo(1);
            assertThat(calculator.calculateLevel(99)).isEqualTo(1);
        }

        @Test
        @DisplayName("1레벨 -> 2레벨 요구 경험치는 100이다 (누적 100 이상 시 2레벨)")
        void calculateLevelTwo() {
            assertThat(calculator.calculateLevel(100)).isEqualTo(2);
            assertThat(calculator.calculateLevel(299)).isEqualTo(2);
        }

        @Test
        /*
         * 1Lv -> 2Lv (100 필요)
         * 2Lv -> 3Lv (200 필요)
         * 누적 300 필요
         */
        @DisplayName("2레벨 -> 3레벨 요구 경험치는 200이다 (누적 300 이상 시 3레벨)")
        void calculateLevelThree() {
            assertThat(calculator.calculateLevel(300)).isEqualTo(3);
            assertThat(calculator.calculateLevel(599)).isEqualTo(3);
        }

        @Test
        /*
         * 1Lv -> 2Lv (100)
         * 2Lv -> 3Lv (200)
         * 3Lv -> 4Lv (300)
         * 누적 600 필요
         */
        @DisplayName("3레벨 -> 4레벨 요구 경험치는 300이다 (누적 600 이상 시 4레벨)")
        void calculateLevelFour() {
            assertThat(calculator.calculateLevel(600)).isEqualTo(4);
            assertThat(calculator.calculateLevel(999)).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("성장 단계(GrowthStage) 판별 검증")
    class GrowthStageDeterminationTest {

        @Test
        @DisplayName("레벨 범위에 따라 정의된 성장 단계가 올바르게 판별된다")
        void determineStageCorrectly() {
            // SEED (1 ~ 2Lv)
            assertThat(calculator.determineGrowthStage(1)).isEqualTo(GrowthStage.SEED);
            assertThat(calculator.determineGrowthStage(2)).isEqualTo(GrowthStage.SEED);

            // SPROUT (3 ~ 5Lv)
            assertThat(calculator.determineGrowthStage(3)).isEqualTo(GrowthStage.SPROUT);
            assertThat(calculator.determineGrowthStage(5)).isEqualTo(GrowthStage.SPROUT);

            // MATURE (6 ~ 9Lv)
            assertThat(calculator.determineGrowthStage(6)).isEqualTo(GrowthStage.MATURE);
            assertThat(calculator.determineGrowthStage(9)).isEqualTo(GrowthStage.MATURE);

            // BLOOM (10 ~ 14Lv)
            assertThat(calculator.determineGrowthStage(10)).isEqualTo(GrowthStage.BLOOM);
            assertThat(calculator.determineGrowthStage(14)).isEqualTo(GrowthStage.BLOOM);

            // FULL_BLOOM (15Lv 이상)
            assertThat(calculator.determineGrowthStage(15)).isEqualTo(GrowthStage.FULL_BLOOM);
            assertThat(calculator.determineGrowthStage(100)).isEqualTo(GrowthStage.FULL_BLOOM);
        }
    }

    @Nested
    @DisplayName("추가 경험치/레벨 연산 기능 검증")
    class AdditionalCalculationTest {

        @Test
        @DisplayName("레벨별 누적 최소 경험치 시작점을 정상적으로 계산한다")
        void calculateMinExpForLevelSuccess() {
            assertThat(calculator.calculateMinExpForLevel(1)).isEqualTo(0);
            assertThat(calculator.calculateMinExpForLevel(2)).isEqualTo(100);
            assertThat(calculator.calculateMinExpForLevel(3)).isEqualTo(300);
            assertThat(calculator.calculateMinExpForLevel(4)).isEqualTo(600);
            assertThat(calculator.calculateMinExpForLevel(0)).isEqualTo(0);
            assertThat(calculator.calculateMinExpForLevel(-5)).isEqualTo(0);
        }

        @Test
        @DisplayName("현재 레벨 내에서 올린 순수 경험치를 정상 계산한다")
        void calculateLevelProgressExpSuccess() {
            // 누적 150 Exp, 현재 레벨 2 => 2레벨 구간 시작점은 100이므로 50 반환
            assertThat(calculator.calculateLevelProgressExp(150, 2)).isEqualTo(50);
            // 누적 50 Exp, 현재 레벨 1 => 1레벨 시작점은 0이므로 50 반환
            assertThat(calculator.calculateLevelProgressExp(50, 1)).isEqualTo(50);
            // 누적 650 Exp, 현재 레벨 4 => 4레벨 시작점은 600이므로 50 반환
            assertThat(calculator.calculateLevelProgressExp(650, 4)).isEqualTo(50);
            // 예외 상황: 누적이 시작점보다 적은 비정상 데이터의 경우 최소 0을 보장
            assertThat(calculator.calculateLevelProgressExp(50, 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("다음 레벨업에 필요한 현재 레벨의 총 요구 경험치를 정상 계산한다")
        void calculateNextLevelRequiredExpSuccess() {
            assertThat(calculator.calculateNextLevelRequiredExp(1)).isEqualTo(100);
            assertThat(calculator.calculateNextLevelRequiredExp(2)).isEqualTo(200);
            assertThat(calculator.calculateNextLevelRequiredExp(3)).isEqualTo(300);
            assertThat(calculator.calculateNextLevelRequiredExp(0)).isEqualTo(100);
            assertThat(calculator.calculateNextLevelRequiredExp(-1)).isEqualTo(100);
        }

        @Test
        @DisplayName("현재 레벨 구간에서의 성장 백분율(%)을 소수점 첫째 자리까지 정확하게 반올림 계산한다")
        void calculateProgressPercentageSuccess() {
            // 누적 150 Exp, 현재 레벨 2 => 2레벨 시작점 100, 현재 구간 경험치 50, 2레벨 요구량 200 => (50/200)*100 = 25.0%
            assertThat(calculator.calculateProgressPercentage(150, 2)).isEqualTo(25.0);
            // 누적 400 Exp, 현재 레벨 3 => 3레벨 시작점 300, 현재 구간 경험치 100, 3레벨 요구량 300 => (100/300)*100 = 33.333... -> 33.3%
            assertThat(calculator.calculateProgressPercentage(400, 3)).isEqualTo(33.3);
            // 누적 410 Exp, 현재 레벨 3 => (110/300)*100 = 36.666... -> 36.7%
            assertThat(calculator.calculateProgressPercentage(410, 3)).isEqualTo(36.7);
            // 비정상 수치에 대한 0.0% 처리
            assertThat(calculator.calculateProgressPercentage(50, 2)).isEqualTo(0.0);
            // 비정상 수치로 현재 레벨 구간 요구량을 초과하더라도 100.0%를 넘지 않는다
            assertThat(calculator.calculateProgressPercentage(999, 2)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("전시용 및 정산용 스트릭(연속 작성일)을 올바르게 계산한다")
        void calculateStreakTests() {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // 1. TIL이 아예 없을 때 -> 둘 다 0일
            assertThat(calculator.calculateStreak(List.of())).isEqualTo(0);
            assertThat(calculator.calculatePreviousStreak(List.of())).isEqualTo(0);

            // 2. 오늘 처음으로 작성했을 때 (now 날짜만 리스트에 존재)
            List<java.time.LocalDateTime> firstDayTimes = List.of(now);
            // 대시보드 전시용은 오늘 포함 1일
            assertThat(calculator.calculateStreak(firstDayTimes)).isEqualTo(1);
            // 경험치 정산용은 어제 기준 0일
            assertThat(calculator.calculatePreviousStreak(firstDayTimes)).isEqualTo(0);

            // 3. 어제 쓰고 오늘 또 썼을 때 (now, now - 1일)
            List<java.time.LocalDateTime> consecutiveTimes = List.of(now, now.minusDays(1));
            // 대시보드 전시용은 2일 연속
            assertThat(calculator.calculateStreak(consecutiveTimes)).isEqualTo(2);
            // 경험치 정산용은 1일 연속 (어제까지 연속 1일)
            assertThat(calculator.calculatePreviousStreak(consecutiveTimes)).isEqualTo(1);

            // 4. 데이터 정합성 문제로 null 발행 시간이 섞여도 계산이 실패하지 않는다
            List<java.time.LocalDateTime> timesWithNull = java.util.Arrays.asList(null, now, now.minusDays(1));
            assertThat(calculator.calculateStreak(timesWithNull)).isEqualTo(2);
            assertThat(calculator.calculatePreviousStreak(timesWithNull)).isEqualTo(1);
        }
    }
}
