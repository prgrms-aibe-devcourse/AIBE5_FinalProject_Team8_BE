package com.Rootin.domain.auth.scheduler;

import com.Rootin.domain.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @InjectMocks
    private RefreshTokenCleanupScheduler refreshTokenCleanupScheduler;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("만료된 Refresh Token과 유예 시간이 지난 회전 토큰을 현재 시각 기준으로 삭제한다")
    void deleteExpiredRefreshTokens_deletesExpiredTokens() {
        // given
        given(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).willReturn(3L);
        given(refreshTokenRepository.deleteByGraceExpiresAtBefore(any(LocalDateTime.class))).willReturn(2L);

        // when
        refreshTokenCleanupScheduler.deleteExpiredRefreshTokens();

        // then
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteByExpiresAtBefore(nowCaptor.capture());
        verify(refreshTokenRepository).deleteByGraceExpiresAtBefore(any(LocalDateTime.class));
        assertThat(nowCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
