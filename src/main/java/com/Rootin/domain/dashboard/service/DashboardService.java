package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.*;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.PotRepository.PotTilDistributionProjection;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository.WateringLogAggregateProjection;
import com.Rootin.domain.garden.repository.WateringLogRepository.DashboardPersonalOverviewProjection;
import com.Rootin.domain.garden.repository.WateringLogRepository.WateringLogDailyAggregateProjection;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.til.repository.TilTagRepository.MonthlyTagCountProjection;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PointLogRepository pointLogRepository;
    private final LevelCalculator levelCalculator;

    public GrassGraphResponse getGrassGraph(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusYears(1).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        List<WateringLogDailyAggregateProjection> dailyLogs =
                wateringLogRepository.aggregateDailyByUserIdAndWateredAtRange(userId, from, to);

        List<GrassCell> cells = dailyLogs.stream()
                .map(log -> {
                    LocalDate date = log.getWateredDate().toLocalDate();
                    int tilCount = toSafeInt(log.getTilCount());
                    int charCount = toSafeInt(log.getContentLength());
                    return new GrassCell(date, tilCount, charCount, GrassCell.resolveLevel(charCount));
                })
                .sorted(Comparator.comparing(GrassCell::date))
                .collect(Collectors.toList());

        List<LocalDate> publishedDates = tilRepository.findDistinctPublishedDatesByUserId(userId, PostStatus.PUBLISHED).stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
        int currentStreak = levelCalculator.calculateStreakFromDates(publishedDates);
        int maxStreak = calculateMaxStreak(cells.stream()
                .map(GrassCell::date)
                .toList());

        return new GrassGraphResponse(cells, currentStreak, maxStreak);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);

        LocalDateTime thisWeekStart = weekStart.atStartOfDay();
        LocalDateTime thisWeekEndExclusive = weekEnd.plusDays(1).atStartOfDay();
        List<WateringLogDailyAggregateProjection> thisWeekLogs =
                wateringLogRepository.aggregateDailyByUserIdAndWateredAtRange(userId, thisWeekStart, thisWeekEndExclusive);

        Map<LocalDate, WateringLogDailyAggregateProjection> byDate = thisWeekLogs.stream()
                .collect(Collectors.toMap(log -> log.getWateredDate().toLocalDate(), log -> log));

        List<WeeklyDataDto> weeklyData = new ArrayList<>();
        for (LocalDate cursor = weekStart; !cursor.isAfter(weekEnd); cursor = cursor.plusDays(1)) {
            WateringLogDailyAggregateProjection dayLog = byDate.get(cursor);
            weeklyData.add(new WeeklyDataDto(
                    cursor,
                    dayLog != null ? toSafeInt(dayLog.getTilCount()) : 0,
                    dayLog != null ? toSafeInt(dayLog.getContentLength()) : 0
            ));
        }

        long lastWeekCount = wateringLogRepository.countByUserIdAndWateredAtGreaterThanEqualAndWateredAtLessThan(
                userId,
                weekStart.minusWeeks(1).atStartOfDay(),
                weekStart.atStartOfDay()
        );

        long thisWeekCount = thisWeekLogs.stream()
                .mapToLong(WateringLogDailyAggregateProjection::getTilCount)
                .sum();

        return new WeeklyStatsResponse(weeklyData, toSafeInt(thisWeekCount), toSafeInt(lastWeekCount));
    }

    public PersonalStatsResponse getPersonalStats(Long userId) {
        DashboardPersonalOverviewProjection overview =
                wateringLogRepository.findPersonalOverviewByUserId(userId, PostStatus.PUBLISHED.name())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        long totalTilCount = overview.getTotalTilCount();
        int totalCharCount = toSafeInt(overview.getTotalContentLength());
        List<LocalDate> wateredDates = wateringLogRepository.findDistinctWateredDatesByUserId(userId).stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
        int totalStudyDays = wateredDates.size();

        List<LocalDate> publishedDates = tilRepository.findDistinctPublishedDatesByUserId(userId, PostStatus.PUBLISHED).stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
        int currentStreak = levelCalculator.calculateStreakFromDates(publishedDates);

        int longestStreak = calculateMaxStreak(publishedDates);

        int currentPoints = Optional.ofNullable(overview.getCurrentPoints()).orElse(0);

        return new PersonalStatsResponse(totalTilCount, totalCharCount, totalStudyDays,
                currentStreak, longestStreak, currentPoints);
    }

    public DistributionResponse getDistribution(Long userId) {
        List<PotTilDistributionProjection> distributions =
                potRepository.findTilDistributionByUserId(userId, PostStatus.PUBLISHED);

        long total = distributions.stream()
                .map(PotTilDistributionProjection::getTilCount)
                .mapToLong(Long::longValue)
                .sum();

        List<DistributionItemDto> items = distributions.stream()
                .map(distribution -> {
                    long count = distribution.getTilCount();
                    double ratio = total == 0 ? 0.0 : Math.round((double) count / total * 1000.0) / 10.0;
                    return new DistributionItemDto(distribution.getPotId(), distribution.getPotTitle(), count, ratio);
                })
                .filter(item -> item.tilCount() > 0)
                .collect(Collectors.toList());

        return new DistributionResponse(items);
    }

    public InterestsResponse getInterests(Long userId, int months) {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L).atStartOfDay();

        List<MonthlyTagCountProjection> tags =
                tilTagRepository.findMonthlyTagCountsSince(userId, PostStatus.PUBLISHED.name(), from);

        Map<String, List<MonthlyTagCountProjection>> byMonth = tags.stream()
                .collect(Collectors.groupingBy(MonthlyTagCountProjection::getMonth));

        List<MonthlyInterestDto> interests = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String month = entry.getKey();
                    List<TagCountDto> topTags = entry.getValue().stream()
                            .sorted(Comparator.comparingLong(MonthlyTagCountProjection::getTagCount).reversed())
                            .limit(5)
                            .map(tag -> new TagCountDto(tag.getTagName(), tag.getTagCount()))
                            .collect(Collectors.toList());
                    return new MonthlyInterestDto(month, topTags);
                })
                .collect(Collectors.toList());

        return new InterestsResponse(interests);
    }

    @Transactional
    public QuestResponse getQuests(Long userId) {
        LocalDate today        = LocalDate.now();
        LocalDateTime todayStart    = today.atStartOfDay();
        // 반열린 구간 [todayStart, tomorrowStart) — datetime(6) microsecond 누락 방지
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Optional<WateringLogAggregateProjection> todayStats = wateringLogRepository
                .aggregateByUserIdAndWateredAtGreaterThanEqualAndWateredAtLessThan(userId, todayStart, tomorrowStart);
        long todayTilCount = todayStats
                .map(WateringLogAggregateProjection::getTilCount)
                .orElse(0L);
        long todayContentLength = todayStats
                .map(WateringLogAggregateProjection::getContentLength)
                .orElse(0L);

        // Q1: 오늘 TIL >= 1개
        boolean q1 = todayTilCount >= 1;

        // Q2: 오늘 TIL에 태그 >= 1개
        long todayTagCount = tilTagRepository.countByUserTodayTil(userId, PostStatus.PUBLISHED, todayStart, tomorrowStart);
        boolean q2 = todayTagCount >= 1;

        // Q3: 오늘 총 글자 수 >= 200
        boolean q3 = todayContentLength >= 200;

        // 달성된 퀘스트에 대해 오늘 첫 달성이면 포인트 지급
        awardQuestPoints(userId, q1, q2, q3, today);

        List<QuestDto> quests = List.of(
                new QuestDto("Q1", "TIL 1개 작성하기", q1, 50),
                new QuestDto("Q2", "TIL에 태그 달기", q2, 30),
                new QuestDto("Q3", "200자 이상 작성", q3, 20)
        );

        int earnedToday = quests.stream().filter(QuestDto::done).mapToInt(QuestDto::point).sum();
        int totalToday  = quests.stream().mapToInt(QuestDto::point).sum();

        return new QuestResponse(quests, earnedToday, totalToday);
    }

    private static final Set<PointLogReason> QUEST_REASONS =
            Set.of(PointLogReason.QUEST_Q1, PointLogReason.QUEST_Q2, PointLogReason.QUEST_Q3);

    private void awardQuestPoints(Long userId, boolean q1, boolean q2, boolean q3, LocalDate today) {
        // 달성된 퀘스트가 없으면 DB 조회 없이 early return
        if (!q1 && !q2 && !q3) return;

        EnumSet<PointLogReason> achievedReasons = EnumSet.noneOf(PointLogReason.class);
        if (q1) achievedReasons.add(PointLogReason.QUEST_Q1);
        if (q2) achievedReasons.add(PointLogReason.QUEST_Q2);
        if (q3) achievedReasons.add(PointLogReason.QUEST_Q3);

        // 이미 지급이 끝난 일반 조회는 락 없이 빠르게 반환합니다.
        Set<PointLogReason> awardedToday =
                pointLogRepository.findQuestReasonsByUserIdAndAwardedDate(userId, today, QUEST_REASONS);
        achievedReasons.removeAll(awardedToday);
        if (achievedReasons.isEmpty()) return;

        awardIfNew(userId, achievedReasons, PointLogReason.QUEST_Q1, 50, today);
        awardIfNew(userId, achievedReasons, PointLogReason.QUEST_Q2, 30, today);
        awardIfNew(userId, achievedReasons, PointLogReason.QUEST_Q3, 20, today);
    }

    private void awardIfNew(Long userId, Set<PointLogReason> achievedReasons,
                             PointLogReason reason, int point, LocalDate awardedDate) {
        if (!achievedReasons.contains(reason)) return;

        // 동시 요청 중 먼저 INSERT에 성공한 요청만 포인트를 지급합니다.
        // 중복 방지는 point_log(user_id, reason, awarded_date) 유니크 인덱스가 담당합니다.
        int inserted = pointLogRepository.insertQuestLogIfAbsent(userId, reason.name(), point, awardedDate);
        if (inserted == 0) return;

        // 로그 삽입에 성공한 첫 요청만 포인트를 원자적으로 증가시킵니다.
        userRepository.incrementPoint(userId, point);
    }

    private int calculateMaxStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;

        List<LocalDate> sorted = dates.stream().sorted().collect(Collectors.toList());
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

    private int toSafeInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }
}
