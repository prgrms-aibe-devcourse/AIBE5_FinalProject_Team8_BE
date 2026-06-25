package com.Rootin.domain.garden.service;

import com.Rootin.domain.garden.dto.GardenInfoResponse;
import com.Rootin.domain.garden.dto.PlantInfoResponse;
import com.Rootin.domain.garden.entity.PlantItem;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PlantItemRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.plant.entity.Plant;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.plant.repository.PlantRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 화분 상세 대시보드 화면(GD-03)을 구성하기 위해 필요한 복합 조회 로직을 담당하는 전담 서비스 클래스입니다.
 * 단일 책임 원칙(SRP)을 준수하여, CRUD 중심의 PotService에서 복합 조회 책임을 분리해 설계했습니다.
 *
 * PotService:
 * - 화분 생성, 목록 조회, 단건 조회, 정보 수정처럼 "화분 자체"의 기본 기능 담당
 *
 * GardenDashboardService:
 * - 화분 + 식물 + TIL 통계 + 물주기 이력 + 레벨 계산 결과를 모아 화면 전용 DTO로 조립
 *
 * 이 서비스는 데이터를 변경하지 않는 조회 API 전용이므로 @Transactional(readOnly = true)를 사용합니다.
 * readOnly 트랜잭션은 JPA가 불필요한 변경 감지를 줄일 수 있고, 의도상 "조회만 한다"는 것을 코드로 표현합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GardenDashboardService {

    private final PotRepository potRepository;
    private final PlantItemRepository plantItemRepository;
    private final PlantRepository plantRepository;
    private final TilRepository tilRepository;
    private final WateringLogRepository wateringLogRepository;
    private final LevelCalculator levelCalculator;

    /**
     * 특정 화분의 상세 대시보드 정보(GardenInfoResponse)를 락 없이 안전하게 조회합니다.
     * [비즈니스 흐름]
     * 1. 화분(Pot) 엔티티 조회 및 사용자 소유권 검증.
     * 2. 화분에 심겨 있는 식물(PlantItem) 마스터 정보 조회.
     * 3. 화분 레벨을 기반으로 현재 도달해야 하는 성장 단계(GrowthStage) 계산 및 식물 마스터 데이터 조회.
     * 4. 데이터 부재 시의 Fallback 정책 수행 (성장 단계 calculatedStage 보존).
     * 5. 레벨 진행률 및 구간 목표치 연산.
     * 6. 사용자가 작성한 누적 발행 완료 TIL 개수 및 마지막 물 준 시각, 스트릭 연속일수 계산 및 DTO 조립.
     *
     * 주의:
     * - 여기서는 비관적 락을 걸지 않습니다. 대시보드는 조회 화면이라 잠금으로 인한 대기 시간이 더 큰 손해가 될 수 있습니다.
     * - 경험치 정산처럼 값이 변경되는 흐름은 TilService.create() -> ExperienceService.applyWatering()에서 처리합니다.
     *
     * @param potId  화분 ID
     * @param userId 사용자 ID
     * @return 대시보드 복합 정보 응답 DTO
     */
    public GardenInfoResponse getGardenDashboard(Long potId, Long userId) {
        // 1. 화분 정보를 조회합니다. (락 미사용)
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.POT_NOT_FOUND));

        // 2. 요청자가 이 화분의 실제 소유주가 맞는지 검증합니다.
        if (!pot.getUserId().equals(userId)) {
            throw CustomException.of(ErrorCode.POT_FORBIDDEN);
        }

        // 3. 현재 화분에 심겨 있고 수확되지 않은 식물(PlantItem)을 조회합니다.
        // PlantItem은 "사용자의 화분에 어떤 식물이 심겨 있는지"를 나타내는 연결 테이블 역할을 합니다.
        PlantItem plantItem = plantItemRepository.findByPotIdAndIsHarvestedFalse(potId)
                .orElseThrow(() -> CustomException.of(ErrorCode.NO_ACTIVE_PLANT));

        // 4. 식물의 기본 정보(Fallback 대비 마스터 데이터)를 획득합니다.
        // Plant는 이미지 URL, 등급, 성장 단계 같은 "식물 마스터 데이터"를 담습니다.
        Plant basePlant = plantRepository.findById(plantItem.getPlantId())
                .orElseThrow(() -> CustomException.of(ErrorCode.PLANT_NOT_FOUND));

        // 5. 현재 식물의 누적 경험치를 토대로 이 식물의 런타임 성장 단계를 판별합니다.
        GrowthStage calculatedStage = levelCalculator.determinePlantGrowthStage(plantItem.getGrowthExp());

        // 6. 해당 성장 단계의 마스터 식물 정보를 조회하여 동적 이미지를 가져옵니다.
        // 예) 기본 씨앗 + COMMON + BLOOM 행이 있으면 BLOOM 단계 이미지를 내려줍니다.
        // 아직 단계별 이미지 데이터가 준비되지 않은 환경에서는 basePlant로 fallback합니다.
        Plant currentStagePlant = plantRepository.findFirstByNameAndGradeAndGrowthStage(
                basePlant.getName(),
                basePlant.getGrade(),
                calculatedStage
        ).orElse(basePlant); // DB에 단계별 정보가 아직 없는 경우, 처음에 심었던 basePlant를 Fallback으로 삼습니다.

        // 6-2. 식물 성장률(%) 및 수확 가능 여부를 계산합니다.
        double growthPercentage = levelCalculator.calculatePlantGrowthPercentage(plantItem.getGrowthExp());
        boolean canHarvest = true;

        // 7. [Fallback 정책 적용]: 이미지는 Fallback을 쓰더라도 growthStage는 연산된 현재 단계를 정확히 노출합니다.
        PlantInfoResponse plantInfo = new PlantInfoResponse(
                currentStagePlant.getName(),
                calculatedStage,
                currentStagePlant.getImageUrl(),
                currentStagePlant.getSilhouetteUrl(),
                growthPercentage,
                canHarvest
        );

        // 8. 레벨 연산기(LevelCalculator)를 통해 현재 레벨 구간의 경험치 상황을 계산합니다.
        int currentLevelExp = levelCalculator.calculateLevelProgressExp(pot.getTotalExp(), pot.getLevel());
        int nextLevelExpRequired = levelCalculator.calculateNextLevelRequiredExp(pot.getLevel());
        double progressPercentage = levelCalculator.calculateProgressPercentage(pot.getTotalExp(), pot.getLevel());

        // 9. 특정 유저가 해당 화분에 발행 완료(PUBLISHED)한 TIL 누적 개수를 조회합니다.
        // DRAFT까지 포함하면 대시보드 통계가 부풀려지므로 반드시 PUBLISHED만 카운트합니다.
        long totalTilCount = tilRepository.countByUserIdAndPotIdAndStatus(userId, potId, PostStatus.PUBLISHED);

        // 10. 특정 유저가 해당 화분에 준 가장 최근 물주기 기록 1건을 조회합니다.
        LocalDateTime lastWateredAt = wateringLogRepository.findLatestWateredAtByUserIdAndPotId(userId, potId)
                .orElse(null);

        // 11. 유저의 TIL 발행 날짜만 조회해 현재 연속 작성일(스트릭)을 계산합니다.
        // 같은 날짜에 여러 글을 작성해도 스트릭에는 날짜 하나만 필요하므로 DISTINCT 날짜 조회로 힙 사용량을 줄입니다.
        List<LocalDate> publishedDates = tilRepository.findDistinctPublishedDatesByUserId(userId, PostStatus.PUBLISHED.name())
                .stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
        int streakDays = levelCalculator.calculateStreakFromDates(publishedDates);

        // 12. 복합 정보 DTO를 조립 반환합니다.
        return new GardenInfoResponse(
                pot.getId(),
                pot.getTitle(),
                pot.getDescription(),
                pot.getLevel(),
                pot.getTotalExp(),
                currentLevelExp,
                nextLevelExpRequired,
                progressPercentage,
                totalTilCount,
                streakDays,
                lastWateredAt,
                plantInfo
        );
    }
}
