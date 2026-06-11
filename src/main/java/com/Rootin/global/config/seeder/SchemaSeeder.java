package com.Rootin.global.config.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaSeeder {

    private static final String INDEX_NAME = "ux_plant_item_one_active_per_pot";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 화분당 활성 PlantItem(isHarvested=false/null)이 1개만 존재하도록 DB 수준에서 보장합니다.
     * MySQL 8.0 함수형 인덱스를 사용해 is_harvested=true인 수확 완료 항목은 제약 대상에서 제외합니다.
     * 이미 인덱스가 존재하면 스킵합니다.
     */
    public void ensureUniqueActivePlantPerPot() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = 'plant_item' " +
                "AND index_name = ?",
                Integer.class, INDEX_NAME
        );

        if (count != null && count > 0) {
            log.debug("인덱스 {} 이미 존재. 스킵.", INDEX_NAME);
            return;
        }

        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX " + INDEX_NAME + " " +
                "ON plant_item ((IF(is_harvested = 0 OR is_harvested IS NULL, pot_id, NULL)))"
        );
        log.info("인덱스 {} 생성 완료 — 화분당 활성 식물 중복 삽입 DB 수준 차단", INDEX_NAME);
    }
}
