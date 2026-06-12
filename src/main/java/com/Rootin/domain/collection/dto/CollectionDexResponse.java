package com.Rootin.domain.collection.dto;

import java.util.List;

public record CollectionDexResponse(DexStats stats, List<DexSection> sections) {}
