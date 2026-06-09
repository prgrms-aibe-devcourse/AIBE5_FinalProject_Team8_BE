package com.Rootin.domain.collection.dto;

public record DexEntry(
        String dexNumber,    // "001" ~ "040"
        int stageIndex,      // 0~4
        String stageName,    // "씨앗" | "새싹" | "잎" | "개화" | "만개"
        String monName,      // null if locked
        boolean collected,
        String harvestedAt,  // "yyyy.MM.dd", null if locked
        String imageUrl      // S3 URL, null if not yet registered
) {}
