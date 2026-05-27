package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.global.annotation.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class PotRepositoryTest {

    @Autowired
    private PotRepository potRepository;

    @Test
    @DisplayName("특정 유저가 보유한 화분 목록을 정상적으로 조회한다")
    void findByUserIdSuccess() {
        // given
        Long userId = 100L;
        Pot pot1 = Pot.builder()
                .userId(userId)
                .title("자바 화분")
                .description("초보용")
                .build();
        Pot pot2 = Pot.builder()
                .userId(userId)
                .title("스프링 화분")
                .description("심화용")
                .build();
        Pot otherPot = Pot.builder()
                .userId(200L)
                .title("다른 사람 화분")
                .build();

        potRepository.save(pot1);
        potRepository.save(pot2);
        potRepository.save(otherPot);

        // when
        List<Pot> pots = potRepository.findByUserId(userId);

        // then
        assertThat(pots).hasSize(2);
        assertThat(pots).extracting(Pot::getTitle)
                .containsExactlyInAnyOrder("자바 화분", "스프링 화분");
    }
}
