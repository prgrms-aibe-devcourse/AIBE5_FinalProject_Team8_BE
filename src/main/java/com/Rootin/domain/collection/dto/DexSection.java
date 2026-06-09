package com.Rootin.domain.collection.dto;

import java.util.List;

public record DexSection(
        String speciesKey,    // "seed" | "shroom" | "cactus" | "fire" | "ice" | "moon" | "bolt" | "rose"
        String speciesLabel,  // "씨앗몬 계열" 등
        boolean rare,
        String numRange,      // "001~005" 등
        List<DexEntry> entries
) {}
