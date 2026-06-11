package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.*;
import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
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
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
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
    private final PointLogRepository pointLogRepository;
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

    @Transactional
    public QuestResponse getQuests(Long userId) {
        LocalDate today        = LocalDate.now();
        LocalDateTime todayStart    = today.atStartOfDay();
        // 반열린 구간 [todayStart, tomorrowStart) — datetime(6) microsecond 누락 방지
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        List<WateringLog> todayLogs = wateringLogRepository
                .findByUserIdAndWateredAtGreaterThanEqualAndWateredAtLessThan(userId, todayStart, tomorrowStart);

        // Q1: 오늘 TIL >= 1개
        boolean q1 = !todayLogs.isEmpty();

        // Q2: 오늘 TIL에 태그 >= 1개
        long todayTagCount = tilTagRepository.countByUserTodayTil(userId, PostStatus.PUBLISHED, todayStart, tomorrowStart);
        boolean q2 = todayTagCount >= 1;

        // Q3: 오늘 총 글자 수 >= 200
        int todayCharCount = todayLogs.stream().mapToInt(WateringLog::getContentLength).sum();
        boolean q3 = todayCharCount >= 200;

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

        // awardedDate 기준으로 오늘 이미 지급된 퀘스트 reason 조회 (createdAt BETWEEN 대신)
        Set<PointLogReason> awardedToday =
                pointLogRepository.findQuestReasonsByUserIdAndAwardedDate(userId, today, QUEST_REASONS);

        // User 풀 로딩 없이 프록시 참조만 사용 (PointLog FK 저장용)
        User userRef = userRepository.getReferenceById(userId);

        awardIfNew(userId, userRef, q1, PointLogReason.QUEST_Q1, 50, awardedToday, today);
        awardIfNew(userId, userRef, q2, PointLogReason.QUEST_Q2, 30, awardedToday, today);
        awardIfNew(userId, userRef, q3, PointLogReason.QUEST_Q3, 20, awardedToday, today);
    }

    private void awardIfNew(Long userId, User userRef, boolean done, PointLogReason reason,
                             int point, Set<PointLogReason> awardedToday, LocalDate awardedDate) {
        if (!done) return;
        if (awardedToday.contains(reason)) return;

        // 원자적 UPDATE — 동시 요청 시 lost update 방지
        userRepository.incrementPoint(userId, point);
        pointLogRepository.save(PointLog.forQuest(userRef, reason, point, awardedDate));
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
