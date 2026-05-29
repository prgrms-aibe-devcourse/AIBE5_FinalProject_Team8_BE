package com.Rootin.domain.gamification.service;

import com.Rootin.domain.gamification.dto.PointLogResponse;
import com.Rootin.domain.gamification.dto.PointSummaryResponse;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    public PointSummaryResponse getPointSummary(Long userId) {
        int currentPoint = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getPoint();
        int totalEarned = pointLogRepository.sumEarnedByUserId(userId);
        int totalUsed = Math.abs(pointLogRepository.sumUsedByUserId(userId));

        return new PointSummaryResponse(currentPoint, totalEarned, totalUsed);
    }

    public Page<PointLogResponse> getPointHistory(Long userId, Pageable pageable) {
        return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PointLogResponse::from);
    }
}
