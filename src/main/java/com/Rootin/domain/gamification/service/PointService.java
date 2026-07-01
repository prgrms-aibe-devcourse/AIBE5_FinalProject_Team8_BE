package com.Rootin.domain.gamification.service;

import com.Rootin.domain.gamification.dto.PointLogResponse;
import com.Rootin.domain.gamification.dto.PointSummaryResponse;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.gamification.repository.PointLogRepository.PointSummaryProjection;
import com.Rootin.global.exception.CustomException;
import com.Rootin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLogRepository pointLogRepository;

    public PointSummaryResponse getPointSummary(Long userId) {
        PointSummaryProjection summary = pointLogRepository.summarizeByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        int currentPoint = summary.getCurrentPoint() != null ? summary.getCurrentPoint() : 0;
        int totalEarned = toSafeInt(summary.getTotalEarned());
        int totalUsed = toSafeInt(summary.getTotalUsed());

        return new PointSummaryResponse(currentPoint, totalEarned, totalUsed);
    }

    public Page<PointLogResponse> getPointHistory(Long userId, Pageable pageable) {
        return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PointLogResponse::from);
    }

    private int toSafeInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }
}
