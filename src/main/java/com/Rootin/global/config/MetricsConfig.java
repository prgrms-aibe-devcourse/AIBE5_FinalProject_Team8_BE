package com.Rootin.global.config;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean
    public ProcessMemoryMetrics processMemoryMetrics() {
        return new ProcessMemoryMetrics();
    }

    @Bean
    public ProcessThreadMetrics processThreadMetrics() {
        return new ProcessThreadMetrics();
    }
}
