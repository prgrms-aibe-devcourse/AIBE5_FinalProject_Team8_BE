package com.Rootin.global.config.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DatabaseSeeder {

    private final PlantMasterSeeder plantMasterSeeder;
    private final UserPotSeeder userPotSeeder;
    private final TilSeeder tilSeeder;
    private final SchemaSeeder schemaSeeder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        log.info("=== DB 시드 데이터 검사 시작 ===");
        schemaSeeder.ensureUniqueActivePlantPerPot();
        plantMasterSeeder.seed();
        userPotSeeder.seed().ifPresent(tilSeeder::seed);
        log.info("=== DB 시드 데이터 완료 ===");
    }
}
