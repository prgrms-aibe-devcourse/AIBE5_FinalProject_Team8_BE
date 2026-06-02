package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.*;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
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
    private final LevelCalculator levelCalculator;

    public GrassGraphResponse getGrassGraph(Long userId, int months) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusMonths(months).atStartOfDay();
        LocalDateTime to   = today.atTime(23, 59, 59);
        List<WateringLog> logs = wateringLogRepository.findByUserIdAndWateredAtBetween(userId, from, to);

        Map<LocalDate, List<WateringLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getWateredAt().toLocalDate()));

        List<GrassCell> cells = byDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<WateringLog> dayLogs = entry.getValue();
                    int tilCount = dayLogs.size();
                    int charCount = dayLogs.stream().mapToInt(WateringLog::getContentLength).sum();
                    return new GrassCell(date, tilCount, charCount, GrassCell.resolveLevel(charCount));
                })
                .sorted(Comparator.comparing(GrassCell::date))
                .collect(Collectors.toList());

        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);
        int maxStreak = calculateMaxStreak(byDate.keySet());

        return new GrassGraphResponse(months, cells, currentStreak, maxStreak);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);

        List<WateringLog> thisWeekLogs = wateringLogRepository.findByUserIdAndWateredAtBetween(
                userId, weekStart.atStartOfDay(), weekEnd.atTime(23, 59, 59));

        Map<LocalDate, List<WateringLog>> byDate = thisWeekLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getWateredAt().toLocalDate()));

        List<WeeklyDataDto> weeklyData = new ArrayList<>();
        for (LocalDate cursor = weekStart; !cursor.isAfter(weekEnd); cursor = cursor.plusDays(1)) {
            List<WateringLog> dayLogs = byDate.getOrDefault(cursor, List.of());
            weeklyData.add(new WeeklyDataDto(
                    cursor,
                    dayLogs.size(),
                    dayLogs.stream().mapToInt(WateringLog::getContentLength).sum()
            ));
        }

        List<WateringLog> lastWeekLogs = wateringLogRepository.findByUserIdAndWateredAtBetween(
                userId,
                weekStart.minusWeeks(1).atStartOfDay(),
                weekEnd.minusWeeks(1).atTime(23, 59, 59)
        );

        return new WeeklyStatsResponse(weeklyData, thisWeekLogs.size(), lastWeekLogs.size());
    }

    public PersonalStatsResponse getPersonalStats(Long userId) {
        long totalTilCount = tilRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);

        List<WateringLog> allLogs = wateringLogRepository.findAllByUserId(userId);
        int totalCharCount  = allLogs.stream().mapToInt(WateringLog::getContentLength).sum();
        int totalStudyDays  = (int) allLogs.stream().map(l -> l.getWateredAt().toLocalDate()).distinct().count();

        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        int currentStreak = levelCalculator.calculateStreak(publishedTimes);

        Set<LocalDate> allDates = allLogs.stream()
                .map(l -> l.getWateredAt().toLocalDate())
                .collect(Collectors.toSet());
        int longestStreak = calculateMaxStreak(allDates);

        int currentPoints = userRepository.findById(userId)
                .map(User::getPoint)
                .orElse(0);

        return new PersonalStatsResponse(totalTilCount, totalCharCount, totalStudyDays,
                currentStreak, longestStreak, currentPoints);
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

    public QuestResponse getQuests(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd   = today.atTime(23, 59, 59);

        List<WateringLog> todayLogs = wateringLogRepository.findByUserIdAndWateredAtBetween(userId, todayStart, todayEnd);

        // Q1: 오늘 TIL >= 1개
        boolean q1 = !todayLogs.isEmpty();

        // Q2: 연속 기록 2일 이상
        List<LocalDateTime> publishedTimes = tilRepository.findPublishedAtByUserId(userId, PostStatus.PUBLISHED);
        boolean q2 = levelCalculator.calculateStreak(publishedTimes) >= 2;

        // Q3: 오늘 총 글자 수 >= 500
        int todayCharCount = todayLogs.stream().mapToInt(WateringLog::getContentLength).sum();
        boolean q3 = todayCharCount >= 500;

        // Q4: 주말이면 TIL >= 1개, 평일이면 자동 달성
        DayOfWeek dow = today.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean q4 = !isWeekend || q1;

        List<QuestDto> quests = List.of(
                new QuestDto("Q1", "TIL 1개 작성하기", q1, 50),
                new QuestDto("Q2", "연속 기록 이어가기", q2, 30),
                new QuestDto("Q3", "500자 이상 작성", q3, 20),
                new QuestDto("Q4", "주말에도 한 줄 기록", q4, 10)
        );

        int earnedToday = quests.stream().filter(QuestDto::done).mapToInt(QuestDto::point).sum();
        int totalToday  = quests.stream().mapToInt(QuestDto::point).sum();

        return new QuestResponse(quests, earnedToday, totalToday);
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
