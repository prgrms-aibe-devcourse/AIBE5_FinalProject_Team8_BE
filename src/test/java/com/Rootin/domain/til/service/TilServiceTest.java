package com.Rootin.domain.til.service;

import com.Rootin.domain.ai.repository.AiResultTilRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.garden.repository.WateringLogRepository;
import com.Rootin.domain.garden.service.ExperienceService;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TilServiceTest {

    @InjectMocks
    private TilService tilService;

    @Mock
    private TilRepository tilRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PotRepository potRepository;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private AiResultTilRepository aiResultTilRepository;

    @Mock
    private WateringLogRepository wateringLogRepository;

    private User owner;
    private Til til;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);

        Pot pot = Pot.builder()
                .userId(1L)
                .title("테스트 화분")
                .level(1)
                .totalExp(0)
                .build();
        ReflectionTestUtils.setField(pot, "id", 10L);

        til = new Til();
        ReflectionTestUtils.setField(til, "id", 100L);
        ReflectionTestUtils.setField(til, "user", owner);
        ReflectionTestUtils.setField(til, "pot", pot);
    }

    // ─── delete() ────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 삭제 — aiResultTil·wateringLog 정리 후 til 삭제, 삭제 순서 보장")
    void delete_success_removesRelatedRecordsInOrder() {
        given(tilRepository.findById(100L)).willReturn(Optional.of(til));

        tilService.delete(100L, 1L);

        // aiResultTil → wateringLog → til 순서 검증
        var inOrder = inOrder(aiResultTilRepository, wateringLogRepository, tilRepository);
        inOrder.verify(aiResultTilRepository).deleteByTilId(100L);
        inOrder.verify(wateringLogRepository).deleteByPostId(100L);
        inOrder.verify(tilRepository).delete(til);
    }

    @Test
    @DisplayName("타인 TIL 삭제 시도 → 403")
    void delete_forbidden_when_not_owner() {
        given(tilRepository.findById(100L)).willReturn(Optional.of(til));

        assertThatThrownBy(() -> tilService.delete(100L, 2L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(aiResultTilRepository, wateringLogRepository);
        verify(tilRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 tilId 삭제 → 404")
    void delete_notFound_when_til_not_exists() {
        given(tilRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tilService.delete(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(aiResultTilRepository, wateringLogRepository);
        verify(tilRepository, never()).delete(any());
    }

}
