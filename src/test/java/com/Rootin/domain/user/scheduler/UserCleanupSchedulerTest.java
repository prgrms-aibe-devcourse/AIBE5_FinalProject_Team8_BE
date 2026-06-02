package com.Rootin.domain.user.scheduler;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

    @InjectMocks
    private UserCleanupScheduler scheduler;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("deletedAt 30일 경과 유저 존재 → deleteAll 호출")
    void permanentlyDeleteExpiredUsers_whenExpiredExists() {
        // given
        User expiredUser = User.builder()
                .email("expired@rootin.com")
                .nickname("탈퇴유저")
                .role(Role.USER)
                .build();
        expiredUser.deactivate(); // deletedAt = now (테스트에서는 cutoff보다 과거라 가정)

        given(userRepository.findByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(List.of(expiredUser));

        // when
        scheduler.permanentlyDeleteExpiredUsers();

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getEmail()).isEqualTo("expired@rootin.com");
    }

    @Test
    @DisplayName("영구 삭제 대상 없음 → deleteAll 미호출")
    void permanentlyDeleteExpiredUsers_whenNoneExpired() {
        // given
        given(userRepository.findByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        scheduler.permanentlyDeleteExpiredUsers();

        // then
        verify(userRepository).findByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("cutoff 기준 LocalDateTime이 now()-30일로 계산되는지 확인")
    void permanentlyDeleteExpiredUsers_cutoffIs30DaysAgo() {
        // given
        given(userRepository.findByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(30).minusSeconds(1);

        // when
        scheduler.permanentlyDeleteExpiredUsers();

        // then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).findByIsDeletedTrueAndDeletedAtBefore(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isAfter(before);
        assertThat(cutoff).isBefore(LocalDateTime.now().minusDays(29));
    }
}
