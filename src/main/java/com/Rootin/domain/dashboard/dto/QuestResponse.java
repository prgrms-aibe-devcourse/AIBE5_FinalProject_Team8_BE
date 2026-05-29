package com.Rootin.domain.dashboard.dto;

import java.util.List;

public record QuestResponse(
        List<QuestDto> quests,
        int earnedToday,
        int totalToday
) {}
