package com.Rootin.domain.dashboard.dto;

public record QuestDto(
        String id,
        String label,
        boolean done,
        int point
) {}
