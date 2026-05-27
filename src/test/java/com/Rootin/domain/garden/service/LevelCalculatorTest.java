package com.Rootin.domain.garden.service;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
