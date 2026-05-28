package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.DistributionItemDto;
import com.Rootin.domain.dashboard.dto.DistributionResponse;
import com.Rootin.domain.dashboard.dto.GrassCell;
import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
import com.Rootin.domain.dashboard.dto.InterestsResponse;
import com.Rootin.domain.dashboard.dto.MonthlyInterestDto;
import com.Rootin.domain.dashboard.dto.PersonalStatsResponse;
import com.Rootin.domain.dashboard.dto.TagCountDto;
import com.Rootin.domain.dashboard.dto.WeeklyStatsResponse;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime to   = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<WateringLog> logs = wateringLogRepository.findByUserIdAndWateredAtBetween(userId, from, to);

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

        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);
        int maxStreak = calculateMaxStreak(byDate.keySet());

        return new GrassGraphResponse(year, cells, currentStreak, maxStreak);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
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
        long totalTilCount = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);

        List<WateringLog> allLogs = wateringLogRepository.findAllByUserId(userId);
        int totalContentLength = allLogs.stream().mapToInt(WateringLog::getContentLength).sum();
        int totalLearningDays  = (int) allLogs.stream().map(l -> l.getWateredAt().toLocalDate()).distinct().count();
        int totalExpGained     = allLogs.stream().mapToInt(WateringLog::getExpGained).sum();

        int totalPointEarned = pointLogRepository.sumEarnedByUserId(userId);

        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);

        Set<LocalDate> allDates = allLogs.stream()
                .map(l -> l.getWateredAt().toLocalDate())
                .collect(Collectors.toSet());
        int maxStreak = calculateMaxStreak(allDates);

        int currentPoints = userRepository.findById(userId)
                .map(user -> user.getPoint())
                .orElse(0);

        return new PersonalStatsResponse(totalTilCount, totalContentLength, totalLearningDays,
                totalExpGained, totalPointEarned, currentStreak, maxStreak, currentPoints);
    }

    public DistributionResponse getDistribution(Long userId) {
        List<Pot> pots = potRepository.findByUserId(userId);
        if (pots.isEmpty()) {
            return new DistributionResponse(List.of());
        }

        long total = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);

        List<DistributionItemDto> items = pots.stream()
                .map(pot -> {
                    long count = tilRepository.countByUserIdAndPotIdAndStatus(userId, pot.getId(), PostStatus.PUBLISHED);
                    double ratio = total == 0 ? 0.0 : Math.round((double) count / total * 1000.0) / 10.0;
                    return new DistributionItemDto(pot.getId(), pot.getTitle(), count, ratio);
                })
                .filter(item -> item.tilCount() > 0)
                .collect(Collectors.toList());

        return new DistributionResponse(items);
    }

    public InterestsResponse getInterests(Long userId, int months) {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L).atStartOfDay();

        List<TilTag> tags = tilTagRepository.findTagsSince(userId, PostStatus.PUBLISHED, from);

        Map<YearMonth, Map<String, Long>> byMonth = tags.stream()
                .collect(Collectors.groupingBy(
                        tt -> YearMonth.from(tt.getTil().getPublishedAt()),
                        Collectors.groupingBy(tt -> tt.getTag().getName(), Collectors.counting())
                ));

        List<MonthlyInterestDto> interests = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String month = entry.getKey().toString();
                    List<TagCountDto> topTags = entry.getValue().entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(5)
                            .map(e -> new TagCountDto(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());
                    return new MonthlyInterestDto(month, topTags);
                })
                .collect(Collectors.toList());

        return new InterestsResponse(interests);
    }

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
