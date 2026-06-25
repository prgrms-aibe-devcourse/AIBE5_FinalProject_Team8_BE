package com.Rootin.domain.gamification.service;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class PointServiceTest {

    @Autowired private PointService pointService;
    @Autowired private PointLogRepository pointLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("포인트 요약은 현재 포인트와 적립/사용 합계를 한 번에 계산한다")
    void getPointSummary_returnsCurrentEarnedAndUsedTotals() {
        User user = userRepository.save(User.builder()
                .email("point-summary@test.com")
                .nickname("포인트요약")
                .point(120)
                .build());

        pointLogRepository.save(PointLog.builder()
                .user(user)
                .reason(PointLogReason.QUEST_Q1)
                .amount(50)
                .awardedDate(LocalDate.now())
                .build());
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .reason(PointLogReason.AI_SUMMARY)
                .amount(-30)
                .build());

        em.flush();
        em.clear();

        var response = pointService.getPointSummary(user.getId());

        assertThat(response.currentPoint()).isEqualTo(120);
        assertThat(response.totalEarned()).isEqualTo(50);
        assertThat(response.totalUsed()).isEqualTo(30);
    }
}
