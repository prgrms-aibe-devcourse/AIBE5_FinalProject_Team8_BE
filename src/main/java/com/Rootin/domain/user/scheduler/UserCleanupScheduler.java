package com.Rootin.domain.user.scheduler;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 유저 영구 삭제 스케줄러
 *
 * 소프트 딜리트(deactivate) 처리된 유저 중 deletedAt 이 30일 이상 경과한 경우 DB에서 영구 삭제한다.
 * 매일 새벽 3시에 실행된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    /**
     * 탈퇴 후 30일 경과 유저 영구 삭제
     *
     * cron: 매일 03:00 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void permanentlyDeleteExpiredUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<User> expiredUsers = userRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        if (expiredUsers.isEmpty()) {
            log.info("[UserCleanupScheduler] 영구 삭제 대상 유저 없음");
            return;
        }

        userRepository.deleteAll(expiredUsers);
        log.info("[UserCleanupScheduler] 영구 삭제 완료 — 대상: {}명", expiredUsers.size());
    }
}
