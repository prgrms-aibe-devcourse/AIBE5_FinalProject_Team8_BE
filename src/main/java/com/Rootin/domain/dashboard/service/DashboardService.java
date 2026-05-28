package com.Rootin.domain.dashboard.service;

import com.Rootin.domain.dashboard.dto.GrassCell;
import com.Rootin.domain.dashboard.dto.GrassGraphResponse;
import com.Rootin.domain.garden.entity.WateringLog;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.LevelCalculator;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
