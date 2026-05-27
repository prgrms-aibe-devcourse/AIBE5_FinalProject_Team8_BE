package com.Rootin.domain.ai.service;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.ai.entity.enums.ToolType;
import com.Rootin.domain.ai.dto.AiResultResponse;
import com.Rootin.domain.ai.dto.AiResultSaveRequest;
import com.Rootin.domain.ai.repository.AiResultRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.user.entity.User;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiResultServiceTest {

    @InjectMocks
    private AiResultService aiResultService;

    @Mock
    private AiResultRepository aiResultRepository;

    @Mock
    private PotRepository potRepository;

    @Mock
    private TilRepository tilRepository;

    private User owner;
    private User other;
    private Pot pot;
    private Til til;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);

        other = new User();
        ReflectionTestUtils.setField(other, "id", 2L);

        pot = Pot.builder()
                .userId(1L)
                .title("테스트 화분")
                .level(1)
                .totalExp(0)
                .build();
        ReflectionTestUtils.setField(pot, "id", 10L);

        til = new Til();
        ReflectionTestUtils.setField(til, "id", 100L);
        ReflectionTestUtils.setField(til, "user", owner);
        ReflectionTestUtils.setField(til, "content", "TIL 본문");
        ReflectionTestUtils.setField(til, "pot", pot);
    }

    // ─── save() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("SUMMARY 타입 - 정상 저장")
    void save_summary_success() {
        AiResultSaveRequest request = new AiResultSaveRequest(ToolType.SUMMARY, 10L, "요약 내용");
        AiResult savedResult = AiResult.builder()
                .user(owner)
                .resultContent("요약 내용")
                .toolType(ToolType.SUMMARY)
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 100L);
        savedResult.addTil(til);

        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of(til));
        given(aiResultRepository.save(any())).willReturn(savedResult);

        AiResultResponse response = aiResultService.save(request, owner);
        assertThat(response.type()).isEqualTo(ToolType.SUMMARY);
        assertThat(response.potId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("QUIZ 타입 - 정상 저장")
    void save_quiz_success() {
        AiResultSaveRequest request = new AiResultSaveRequest(ToolType.QUIZ, 10L, "문제 JSON");
        AiResult savedResult = AiResult.builder()
                .user(owner)
                .resultContent("문제 JSON")
                .toolType(ToolType.QUIZ)
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 101L);
        savedResult.addTil(til);

        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of(til));
        given(aiResultRepository.save(any())).willReturn(savedResult);

        assertThat(aiResultService.save(request, owner).type()).isEqualTo(ToolType.QUIZ);
    }

    @Test
    @DisplayName("타인 화분에 저장 시도 → 403")
    void save_forbidden_when_not_owner() {
        Pot ownerPot = Pot.builder().userId(1L).title("오너 화분").level(1).totalExp(0).build();
        ReflectionTestUtils.setField(ownerPot, "id", 10L);
        given(potRepository.findById(10L)).willReturn(Optional.of(ownerPot));

        assertThatThrownBy(() -> aiResultService.save(
                new AiResultSaveRequest(ToolType.SUMMARY, 10L, "내용"), other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 화분 → 404")
    void save_notFound_when_pot_not_exists() {
        given(potRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiResultService.save(
                new AiResultSaveRequest(ToolType.SUMMARY, 999L, "내용"), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("화분에 TIL 없음 → 404")
    void save_notFound_when_no_tils_in_pot() {
        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(tilRepository.findByUserIdAndPotIdAndStatus(1L, 10L, PostStatus.PUBLISHED))
                .willReturn(List.of());

        assertThatThrownBy(() -> aiResultService.save(
                new AiResultSaveRequest(ToolType.SUMMARY, 10L, "내용"), owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── getResults() ────────────────────────────────────────────────

    @Test
    @DisplayName("potId 없이 조회 → 전체 결과 반환")
    void getResults_without_potId_returns_all() {
        AiResult r1 = AiResult.builder().user(owner)
                .resultContent("요약1").toolType(ToolType.SUMMARY).build();
        AiResult r2 = AiResult.builder().user(owner)
                .resultContent("문제1").toolType(ToolType.QUIZ).build();
        ReflectionTestUtils.setField(r1, "id", 1L);
        ReflectionTestUtils.setField(r2, "id", 2L);
        r1.addTil(til);
        r2.addTil(til);

        given(aiResultRepository.findAllByUser(owner)).willReturn(List.of(r1, r2));

        assertThat(aiResultService.getResults(owner, null)).hasSize(2);
    }

    @Test
    @DisplayName("potId 기준 필터링 → 해당 화분 결과만 반환")
    void getResults_with_potId_returns_filtered() {
        AiResult r = AiResult.builder().user(owner)
                .resultContent("요약").toolType(ToolType.SUMMARY).build();
        ReflectionTestUtils.setField(r, "id", 1L);
        r.addTil(til);

        given(potRepository.findById(10L)).willReturn(Optional.of(pot));
        given(aiResultRepository.findAllByUserAndPotId(owner, 10L)).willReturn(List.of(r));

        List<AiResultResponse> results = aiResultService.getResults(owner, 10L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).potId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("타인 화분으로 조회 시도 → 403")
    void getResults_forbidden_when_not_owner_potId() {
        Pot ownerPot = Pot.builder().userId(1L).title("오너 화분").level(1).totalExp(0).build();
        ReflectionTestUtils.setField(ownerPot, "id", 10L);
        given(potRepository.findById(10L)).willReturn(Optional.of(ownerPot));

        assertThatThrownBy(() -> aiResultService.getResults(other, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 potId → 404")
    void getResults_notFound_when_pot_not_exists() {
        given(potRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiResultService.getResults(owner, 999L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── delete() ────────────────────────────────────────────────────

    @Test
    @DisplayName("본인 결과 삭제 → 정상 삭제")
    void delete_success() {
        AiResult aiResult = AiResult.builder().user(owner)
                .resultContent("요약").toolType(ToolType.SUMMARY).build();
        ReflectionTestUtils.setField(aiResult, "id", 1L);
        aiResult.addTil(til);

        given(aiResultRepository.findById(1L)).willReturn(Optional.of(aiResult));

        aiResultService.delete(1L, owner);

        verify(aiResultRepository).delete(aiResult);
    }

    @Test
    @DisplayName("타인 결과 삭제 시도 → 403")
    void delete_forbidden_when_not_owner() {
        AiResult aiResult = AiResult.builder().user(owner)
                .resultContent("요약").toolType(ToolType.SUMMARY).build();
        ReflectionTestUtils.setField(aiResult, "id", 1L);
        aiResult.addTil(til);

        given(aiResultRepository.findById(1L)).willReturn(Optional.of(aiResult));

        assertThatThrownBy(() -> aiResultService.delete(1L, other))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 resultId 삭제 → 404")
    void delete_notFound_when_result_not_exists() {
        given(aiResultRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiResultService.delete(999L, owner))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
