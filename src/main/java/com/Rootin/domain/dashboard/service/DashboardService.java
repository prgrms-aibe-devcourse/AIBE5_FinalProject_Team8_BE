package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.GrassCell;
import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
import com.Rootin.domain.dashboard.dto.InterestDistributionResponse;
import com.Rootin.domain.dashboard.dto.PersonalStatsResponse;
import com.Rootin.domain.dashboard.dto.PotInterestDto;
import com.Rootin.domain.dashboard.dto.TagCountByPotDto;
import com.Rootin.domain.dashboard.dto.WeeklyStatsResponse;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.plant.entity.enums.GrowthStage;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WateringLogRepository wateringLogRepository;
    private final TilRepository tilRepository;
    private final TilTagRepository tilTagRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final LevelCalculator levelCalculator;

    public GrassGraphResponse getGrassGraph(Long userId, int year) {
        // 1. 해당 연도 전체 WateringLog 조회
        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<WateringLog> logs = wateringLogRepository.findByUserIdAndWateredAtBetween(userId, from, to);

        // 2. 날짜별 그루핑 후 GrassCell 목록 생성
        Map<LocalDate, List<WateringLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getWateredAt().toLocalDate()));

        List<GrassCell> cells = byDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<WateringLog> dayLogs = entry.getValue();
                    int count = dayLogs.size();
                    int totalContentLength = dayLogs.stream().mapToInt(WateringLog::getContentLength).sum();
                    return new GrassCell(date, count, totalContentLength, GrassCell.resolveLevel(totalContentLength));
                })
                .sorted(Comparator.comparing(GrassCell::date))
                .collect(Collectors.toList());

        // 3. 현재 연속 작성일 — 전체 발행 기록 기준 (연도 경계를 넘는 스트릭 정확히 반영)
        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);

        // 4. 해당 연도 내 최대 연속 작성일
        int maxStreak = calculateMaxStreak(byDate.keySet());

        return new GrassGraphResponse(year, cells, currentStreak, maxStreak);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        // 이번 주 월요일 ~ 일요일 범위 계산
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd   = today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);

        List<WateringLog> weeklyLogs = wateringLogRepository.findByUserIdAndWateredAtBetween(userId, weekStart, weekEnd);

        int weeklyTilCount      = weeklyLogs.size();
        int weeklyExpGained     = weeklyLogs.stream().mapToInt(WateringLog::getExpGained).sum();
        int weeklyPointGained   = weeklyLogs.stream().mapToInt(l -> l.getPointGained() != null ? l.getPointGained() : 0).sum();
        int weeklyContentLength = weeklyLogs.stream().mapToInt(WateringLog::getContentLength).sum();

        return new WeeklyStatsResponse(weeklyTilCount, weeklyExpGained, weeklyPointGained, weeklyContentLength);
    }

    public PersonalStatsResponse getPersonalStats(Long userId) {
        // TIL 총 개수
        long totalTilCount = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);

        // WateringLog 전체 조회 후 Java 집계
        List<WateringLog> allLogs = wateringLogRepository.findAllByUserId(userId);
        int totalContentLength = allLogs.stream().mapToInt(WateringLog::getContentLength).sum();
        int totalLearningDays  = (int) allLogs.stream().map(l -> l.getWateredAt().toLocalDate()).distinct().count();
        int totalExpGained     = allLogs.stream().mapToInt(WateringLog::getExpGained).sum();

        // 총 적립 포인트 (PointLog 기준)
        int totalPointEarned = pointLogRepository.sumEarnedByUserId(userId);

        // 현재 스트릭 — 전체 발행 기록 기준
        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);

        // 전체 기간 최대 스트릭
        Set<LocalDate> allDates = allLogs.stream()
                .map(l -> l.getWateredAt().toLocalDate())
                .collect(Collectors.toSet());
        int maxStreak = calculateMaxStreak(allDates);

        // 현재 포인트 잔액
        int currentPoints = userRepository.findById(userId)
                .map(user -> user.getPoint())
                .orElse(0);

        return new PersonalStatsResponse(totalTilCount, totalContentLength, totalLearningDays,
                totalExpGained, totalPointEarned, currentStreak, maxStreak, currentPoints);
    }

    public InterestDistributionResponse getInterestDistribution(Long userId) {
        List<Pot> pots = potRepository.findByUserId(userId);
        if (pots.isEmpty()) {
            return new InterestDistributionResponse(List.of());
        }

        // 화분별 태그 빈도를 단일 쿼리로 조회 (COUNT DESC 정렬 보장)
        List<TagCountByPotDto> tagCounts = tilTagRepository.findTagCountsByUserAndStatus(userId, PostStatus.PUBLISHED);

        // potId 기준으로 그루핑, 태그명 최대 5개만 추출 (이미 COUNT 내림차순 정렬됨)
        Map<Long, List<String>> topTagsByPot = tagCounts.stream()
                .collect(Collectors.groupingBy(
                        TagCountByPotDto::getPotId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .limit(5)
                                        .map(TagCountByPotDto::getTagName)
                                        .collect(Collectors.toList())
                        )
                ));

        List<PotInterestDto> potDtos = pots.stream()
                .map(pot -> {
                    long tilCount = tilRepository.countByUserIdAndPotIdAndStatus(userId, pot.getId(), PostStatus.PUBLISHED);
                    GrowthStage growthStage = levelCalculator.determineGrowthStage(pot.getLevel());
                    List<String> topTags = topTagsByPot.getOrDefault(pot.getId(), List.of());
                    return new PotInterestDto(pot.getId(), pot.getTitle(), tilCount, pot.getLevel(), growthStage, topTags);
                })
                .collect(Collectors.toList());

        return new InterestDistributionResponse(potDtos);
    }

    // 날짜 Set을 오름차순 정렬 후 연속 구간의 최댓값 계산
    private int calculateMaxStreak(Set<LocalDate> dateSet) {
        if (dateSet.isEmpty()) return 0;

        List<LocalDate> sorted = dateSet.stream().sorted().collect(Collectors.toList());
        int max = 1;
        int current = 1;

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
                if (current > max) max = current;
            } else {
                current = 1;
            }
        }

        return max;
    }
}
