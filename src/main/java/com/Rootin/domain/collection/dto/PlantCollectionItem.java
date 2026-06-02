package com.Rootin.domain.collection.dto;

import java.util.Map;

public record PlantCollectionItem(
        String plantType,               // "기본 씨앗"
        String species,                 // FE 픽셀 아트 키: "seed" | "moonlight" | "mushroom"
        String rarity,                  // "rare" | "common"
        String state,                   // "growing" | "harvested" | "locked"
        String currentStage,            // FE 단계 키: "seed"|"sprout"|"leaf"|"bloom"|"full" (locked이면 null)
        Integer potLevel,
        String potTitle,
        Integer round,                  // 이 화분에서 몇 번째 식물인지
        String startedAt,               // "yyyy.MM.dd"
        String harvestedAt,             // "yyyy.MM.dd" (수확 안 했으면 null)
        Map<String, String> stageDates, // {"씨앗":"MM.dd", "새싹":"MM.dd", ...} 미도달은 null
        String imageUrl
) {}
