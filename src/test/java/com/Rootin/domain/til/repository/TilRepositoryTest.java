package com.Rootin.domain.til.repository;

import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Tag;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.repository.UserRepository;
import com.Rootin.global.annotation.H2RepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@H2RepositoryTest
class TilRepositoryTest {

    @Autowired
    private TilRepository tilRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PotRepository potRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TilTagRepository tilTagRepository;

    private User user;
    private Pot pot1;
    private Pot pot2;

    @BeforeEach
    void setUp() {
        tilTagRepository.deleteAllInBatch();
        tilRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        potRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        user = userRepository.save(User.builder()
                .email("test@test.com")
                .nickname("tester")
                .role(Role.USER)
                .point(0)
                .build());

        pot1 = potRepository.save(Pot.builder()
                .userId(user.getId())
                .title("자바 화분")
                .level(1)
                .totalExp(0)
                .build());

        pot2 = potRepository.save(Pot.builder()
                .userId(user.getId())
                .title("스프링 화분")
                .level(1)
                .totalExp(0)
                .build());
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private Til saveTil(String title, String content, Pot pot) {
        Til til = Til.create(user, title, content, pot);
        return tilRepository.save(til);
    }

    private void attachTag(Til til, String tagName) {
        Tag tag = tagRepository.findByNameIn(List.of(tagName)).stream()
                .findFirst()
                .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
        tilTagRepository.save(TilTag.of(til, tag));
    }

    private PageRequest latestPage() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // ─── keyword 필터 ────────────────────────────────────────────────

    @Test
    @DisplayName("keyword 필터 — 제목에 키워드 포함된 TIL만 반환")
    void findByFilters_keyword_matchesTitle() {
        saveTil("Java 입문", "자바 기초 내용", pot1);
        saveTil("Spring 입문", "스프링 기초 내용", pot1);
        saveTil("JPA 심화", "JPA 고급", pot1);

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, "입문", null, latestPage());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Til::getTitle)
                .containsExactlyInAnyOrder("Java 입문", "Spring 입문");
    }

    @Test
    @DisplayName("keyword 없음 — 전체 TIL 반환")
    void findByFilters_noKeyword_returnsAll() {
        saveTil("TIL 1", "내용1", pot1);
        saveTil("TIL 2", "내용2", pot1);

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, null, null, latestPage());

        assertThat(result.getContent()).hasSize(2);
    }

    // ─── tag 필터 ────────────────────────────────────────────────────

    @Test
    @DisplayName("tag 필터 — 해당 태그가 붙은 TIL만 반환")
    void findByFilters_tag_matchesTaggedTil() {
        Til tilJava = saveTil("Java TIL", "자바 내용", pot1);
        Til tilSpring = saveTil("Spring TIL", "스프링 내용", pot1);
        attachTag(tilJava, "java");
        attachTag(tilSpring, "spring");

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, null, "java", latestPage());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java TIL");
    }

    @Test
    @DisplayName("tag 필터 — 매칭 태그 없으면 빈 결과")
    void findByFilters_tag_noMatch_returnsEmpty() {
        Til til = saveTil("TIL", "내용", pot1);
        attachTag(til, "java");

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, null, "python", latestPage());

        assertThat(result.getContent()).isEmpty();
    }

    // ─── potId + keyword 복합 필터 ──────────────────────────────────

    @Test
    @DisplayName("potId + keyword 복합 필터 — 특정 화분 내에서 키워드 검색")
    void findByFilters_potIdAndKeyword() {
        saveTil("Java 입문", "자바 내용", pot1);      // pot1, 키워드 match
        saveTil("Java 심화", "자바 심화 내용", pot2); // pot2, 키워드 match 이지만 화분 불일치
        saveTil("Spring TIL", "스프링 내용", pot1);   // pot1, 키워드 불일치

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, pot1.getId(), "Java", null, latestPage());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java 입문");
    }

    @Test
    @DisplayName("tag가 없는 TIL — tag 필터 없을 때 정상 조회됨 (DISTINCT 검증)")
    void findByFilters_noTag_tilWithoutTagIsIncluded() {
        saveTil("태그없는 TIL", "내용", pot1);

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, null, null, latestPage());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("여러 태그 붙은 TIL — tag 필터 시 중복 없이 1건만 반환 (DISTINCT 검증)")
    void findByFilters_multiTagTil_distinctResult() {
        Til til = saveTil("멀티태그 TIL", "내용", pot1);
        attachTag(til, "java");
        attachTag(til, "spring");

        Page<Til> result = tilRepository.findByFilters(
                user.getId(), PostStatus.PUBLISHED, null, null, "java", latestPage());

        assertThat(result.getContent()).hasSize(1);
    }
}
