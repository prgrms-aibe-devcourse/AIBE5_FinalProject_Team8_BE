package com.Rootin.domain.auth.scheduler;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token 정리 스케줄러
 *
 * Access Token 재발급 시 Refresh Token을 회전하되, 동시 재발급 요청을 위해 짧은 Grace Period를 둔다.
 * 더 이상 사용할 수 없는 만료 토큰과 Grace Period가 끝난 회전 토큰은 배치로 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 Refresh Token 삭제
     *
     * cron: 매일 04:00 실행
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void deleteExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        long deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(now)
                + refreshTokenRepository.deleteByGraceExpiresAtBefore(now);

        if (deletedCount == 0) {
            log.info("[RefreshTokenCleanupScheduler] 만료 Refresh Token 없음");
            return;
        }

        log.info("[RefreshTokenCleanupScheduler] 만료 Refresh Token 삭제 완료 — 대상: {}개", deletedCount);
    }
}
