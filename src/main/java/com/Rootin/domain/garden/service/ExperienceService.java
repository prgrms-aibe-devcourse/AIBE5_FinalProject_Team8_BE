package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TIL 작성 시 발생하는 경험치 적립, 레벨 계산, 포인트 적립 및 물주기 로그(WateringLog) 기록을
 * 총괄하여 수행하는 핵심 비즈니스 서비스 클래스입니다.
 *
 * 이 서비스는 "쓰기 작업"을 담당하므로 클래스 레벨에 @Transactional을 적용했습니다.
 * applyWatering() 안에서 화분 경험치 변경, 사용자 포인트 변경, 포인트 이력 저장, 물주기 이력 저장이
 * 하나의 트랜잭션으로 묶입니다. 중간에 예외가 발생하면 전체 변경이 롤백되어 데이터가 어긋나는 상황을 막습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExperienceService {

    private final UserRepository userRepository;
    private final WateringLogRepository wateringLogRepository;
    private final TilRepository tilRepository;
    private final LevelCalculator levelCalculator;
    private final PlantItemRepository plantItemRepository;

    /**
     * TIL 작성 완료 이벤트 시 트리거되는 물주기 비즈니스 로직입니다.
     * 경험치/포인트를 계산하고 화분 레벨 및 사용자 포인트 상태를 변경한 뒤 물주기 이력을 DB에 기록합니다.
     *
     * 호출 위치:
     * - TilService.create()
     * - TIL이 PUBLISHED 상태로 저장된 직후 호출됩니다.
     *
     * 중요한 설계 의도:
     * - 임시저장(DRAFT)은 경험치 대상이 아니므로 이 메서드를 호출하지 않습니다.
     * - 같은 TIL(postId)로 두 번 경험치를 받으면 안 되므로 WateringLog 존재 여부와 DB unique 제약으로 이중 방어합니다.
     * - pot은 TilService에서 비관적 락으로 조회한 엔티티를 넘겨받습니다. 그래서 이 메서드는 같은 트랜잭션 안에서
     *   잠긴 화분의 경험치를 안전하게 변경하는 역할만 수행합니다.
     *
     * @param userId        TIL을 작성한 사용자 ID
     * @param pot           물이 공급될 대상 화분 엔티티 객체
     * @param contentLength 작성된 TIL 본문 글자 수
     * @param tilId         생성된 TIL 포스트 ID
     */
    public void applyWatering(Long userId, Pot pot, int contentLength, Long tilId) {
        log.info("=== 물주기 비즈니스 로직 기동 (User: {}, Pot: {}, TIL: {}) ===", userId, pot.getId(), tilId);

        // 1. 동일 TIL 포스트에 대한 물주기 중복 적립 방지 검사.
        // 애플리케이션 레벨에서 먼저 막고, WateringLog.post_id unique 제약으로 DB 레벨에서도 한 번 더 막습니다.
        if (wateringLogRepository.existsByPostId(tilId)) {
            throw CustomException.of(ErrorCode.ALREADY_WATERED_TODAY);
        }

        // 2. 대상 TIL 포스트 존재 여부 및 유저 소유권, 화분 매핑 일치 검증.
        // 클라이언트가 userId 또는 potId를 잘못 보내더라도, 실제 저장된 TIL 기준으로 다시 검증합니다.
        Til til = tilRepository.findById(tilId)
                .orElseThrow(() -> CustomException.notFound("TIL을 찾을 수 없습니다."));

        if (!til.getUser().getId().equals(userId)) {
            throw CustomException.forbidden("해당 TIL에 대한 권한이 없습니다.");
        }

        if (!til.getPot().getId().equals(pot.getId())) {
            throw CustomException.badRequest("TIL과 화분의 정보가 일치하지 않습니다.");
        }

        // 3. 유저 정보 조회 및 화분 소유권 검증.
        // 포인트를 실제 User 엔티티에 더해야 하므로 User를 영속 상태로 조회합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (!pot.getUserId().equals(userId)) {
            throw CustomException.forbidden("해당 화분에 대한 권한이 없습니다.");
        }

        // 4. 연속 작성일(스트릭) 계산.
        // 경험치 보너스는 "오늘 작성한 글"을 포함하지 않고, 어제까지의 연속 기록을 기준으로 계산합니다.
        // 예) 첫 TIL 작성일에는 이전 기록이 없으므로 0일 스트릭 -> 보너스 없음.
        // 예) 어제도 작성했고 오늘도 작성했다면 1일 스트릭 -> 5% 보너스.
        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int streakDays = levelCalculator.calculatePreviousStreak(publishedTimes);
        log.info("조회된 이전 연속 작성일 수 (Streak Days): {}일", streakDays);

        // 5. 경험치 획득량 계산.
        // 순수 계산은 LevelCalculator에 위임하여, 이 서비스는 "조회-검증-상태변경-저장" 흐름에 집중합니다.
        // 포인트는 TIL 작성 시 지급하지 않으며, DashboardService의 퀘스트 달성 시점에 지급됩니다.
        int gainedExp = levelCalculator.calculateExperience(contentLength, streakDays);
        double appliedMultiplier = levelCalculator.calculateStreakMultiplier(streakDays);
        log.info("획득 경험치: {} Exp (글자 수: {}, 배율: {}x)", gainedExp, contentLength, appliedMultiplier);

        // 6. 화분 경험치 가산 및 레벨 계산.
        // WateringLog에 전/후 상태를 남겨야 하므로 변경 전 값을 먼저 백업합니다.
        int beforePotLevel = pot.getLevel();
        int beforeTotalExp = pot.getTotalExp();

        int afterTotalExp = beforeTotalExp + gainedExp;
        int afterPotLevel = levelCalculator.calculateLevel(afterTotalExp);

        // Pot 엔티티 내부 메서드를 통해 상태를 바꾸면, 변경 책임이 엔티티에 모여서 추후 검증 로직을 넣기 쉽습니다.
        pot.updateExperienceAndLevel(gainedExp, afterPotLevel);
        log.info("화분 레벨 변동: {} Lv -> {} Lv (누적 경험치: {} -> {})", beforePotLevel, afterPotLevel, beforeTotalExp, afterTotalExp);

        // [새 정책] 식물 개별 경험치 가산.
        PlantItem plantItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(pot.getId())
                .orElseThrow(() -> CustomException.of(ErrorCode.NO_ACTIVE_PLANT));
        int beforePlantExp = plantItem.getGrowthExp();
        plantItem.increaseGrowthExp(gainedExp);
        log.info("식물 경험치 변동: {} Exp -> {} Exp (획득 경험치: {})", beforePlantExp, plantItem.getGrowthExp(), gainedExp);

        // 7. 물주기 상세 이력(WateringLog) 저장.
        // 포인트는 오늘의 목표(퀘스트) 달성 시 DashboardService에서 지급됩니다.
        // 대시보드의 최근 물주기 시각, 운영 중 정산 검증, 사용자 성장 히스토리 분석에 쓰입니다.
        WateringLog wateringLog = WateringLog.builder()
                .userId(userId)
                .potId(pot.getId())
                .postId(tilId)
                .expGained(gainedExp)
                .pointGained(0)
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


}
