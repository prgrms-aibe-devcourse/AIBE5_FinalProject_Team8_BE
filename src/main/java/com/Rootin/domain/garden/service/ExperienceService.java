package com.Rootin.domain.garden.service;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TIL 작성 시 발생하는 경험치 적립, 레벨 계산, 포인트 적립 및 물주기 로그(WateringLog) 기록을
 * 총괄하여 수행하는 핵심 비즈니스 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExperienceService {

    private final UserRepository userRepository;
    private final PotRepository potRepository;
    private final WateringLogRepository wateringLogRepository;
    private final PointLogRepository pointLogRepository;
    private final TilRepository tilRepository;
    private final LevelCalculator levelCalculator;

    /**
     * TIL 작성 완료 이벤트 시 트리거되는 물주기 비즈니스 로직입니다.
     * 경험치/포인트를 계산하고 화분 레벨 및 사용자 포인트 상태를 변경한 뒤 물주기 이력을 DB에 기록합니다.
     *
     * @param userId        TIL을 작성한 사용자 ID
     * @param pot           물이 공급될 대상 화분 엔티티 객체
     * @param contentLength 작성된 TIL 본문 글자 수
     * @param tilId         생성된 TIL 포스트 ID
     */
    public void applyWatering(Long userId, Pot pot, int contentLength, Long tilId) {
        log.info("=== 물주기 비즈니스 로직 기동 (User: {}, Pot: {}, TIL: {}) ===", userId, pot.getId(), tilId);

        // 1. 동일 TIL 포스트에 대한 물주기 중복 적립 방지 검사
        if (wateringLogRepository.existsByPostId(tilId)) {
            throw CustomException.badRequest("이미 물주기가 완료된 TIL입니다.");
        }

        // 2. 대상 TIL 포스트 존재 여부 및 유저 소유권, 화분 매핑 일치 검증
        com.Rootin.domain.til.entity.Til til = tilRepository.findById(tilId)
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        if (!til.getUser().getId().equals(userId)) {
            throw CustomException.forbidden("해당 TIL에 대한 권한이 없습니다.");
        }

        if (!til.getPot().getId().equals(pot.getId())) {
            throw CustomException.badRequest("TIL과 화분의 정보가 일치하지 않습니다.");
        }

        // 3. 유저 정보 조회 및 소유권 검증 (화분은 파라미터로 이미 제공됨)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 대한 권한이 없습니다.");
        }

        // 2. 연속 작성일(스트릭) 계산
        // 유저가 지금까지 발행한 모든 TIL의 날짜 목록을 조회해 어제 날짜를 기준으로 연속된 작성일수를 구합니다.
        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int streakDays = calculateStreak(publishedTimes);
        log.info("조회된 이전 연속 작성일 수 (Streak Days): {}일", streakDays);

        // 3. 경험치 및 포인트 획득량 계산
        int gainedExp = levelCalculator.calculateExperience(contentLength, streakDays);
        int gainedPoint = levelCalculator.calculatePoints(gainedExp);
        double appliedMultiplier = levelCalculator.calculateStreakMultiplier(streakDays);
        log.info("획득 경험치: {} Exp (글자 수: {}, 배율: {}x), 적립 포인트: {} P", gainedExp, contentLength, appliedMultiplier, gainedPoint);

        // 4. 화분 경험치 가산 및 레벨 계산 (레벨업 처리)
        int beforePotLevel = pot.getLevel();
        int beforeTotalExp = pot.getTotalExp();
        
        int afterTotalExp = beforeTotalExp + gainedExp;
        int afterPotLevel = levelCalculator.calculateLevel(afterTotalExp);
        
        pot.updateExperienceAndLevel(gainedExp, afterPotLevel);
        log.info("화분 레벨 변동: {} Lv -> {} Lv (누적 경험치: {} -> {})", beforePotLevel, afterPotLevel, beforeTotalExp, afterTotalExp);

        // 5. 유저 포인트 가산 및 포인트 변동 이력(PointLog) 저장
        user.addPoint(gainedPoint);
        if (gainedPoint > 0) {
            PointLog pointLog = PointLog.builder()
                    .user(user)
                    .reason(PointLogReason.TIL_WRITE)
                    .amount(gainedPoint)
                    .build();
            pointLogRepository.save(pointLog);
        }

        // 6. 물주기 상세 이력(WateringLog) 저장
        WateringLog wateringLog = WateringLog.builder()
                .userId(userId)
                .potId(pot.getId())
                .postId(tilId)
                .expGained(gainedExp)
                .pointGained(gainedPoint)
                .contentLength(contentLength)
                .streakDays(streakDays)
                .appliedMultiplier(appliedMultiplier)
                .beforePotLevel(beforePotLevel)
                .afterPotLevel(afterPotLevel)
                .beforeTotalExp(beforeTotalExp)
                .afterTotalExp(afterTotalExp)
                .build();

        wateringLogRepository.save(wateringLog);
        log.info("물주기 상세 이력 저장 성공 (Log ID: {})", wateringLog.getId());
    }

    /**
     * 유저의 전체 TIL 발행 일자 목록을 기반으로 어제 날짜부터 역순으로 하루씩 차감하며 연속 작성일(스트릭)을 계산합니다.
     *
     * @param publishedTimes 유저가 작성한 TIL들의 발행 시간 목록
     * @return 어제까지의 연속 작성일 수 (최소 0)
     */
    public int calculateStreak(List<LocalDateTime> publishedTimes) {
        if (publishedTimes == null || publishedTimes.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        // 날짜 단위 조회를 빠르게 처리하기 위해 Set으로 변환하여 O(1) 검색 속도를 보장합니다.
        Set<LocalDate> dateSet = publishedTimes.stream()
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        // 오늘(today) 날짜가 작성 목록에 존재한다면 오늘부터 역산하고, 없으면 어제(yesterday)부터 역산합니다.
        LocalDate checkDate = dateSet.contains(today) ? today : today.minusDays(1);

        // 어제도 오늘도 안 쓴 상태라면 스트릭은 0일입니다.
        if (!dateSet.contains(checkDate)) {
            return 0;
        }

        // 하루씩 거꾸로 올라가며 연속 작성이 이어졌는지 O(1) 조회를 통해 판별합니다.
        int streak = 0;
        
        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }
}
