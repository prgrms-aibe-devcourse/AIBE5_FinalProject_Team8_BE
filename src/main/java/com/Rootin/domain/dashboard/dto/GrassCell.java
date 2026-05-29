package com.Rootin.domain.dashboard.dto;

import java.time.LocalDate;

/**
 * 잔디 그래프의 날짜 한 칸을 나타내는 DTO.
 * level은 해당 날 글자 수 합산 기준 색상 농도 (0: 미작성, 1~4: 작성량 단계)
 */
public record GrassCell(
        LocalDate date,
        int tilCount,
        int charCount,
        int level
) {
    // charCount 기준 농도 분기: 0 / 1~300 / 301~700 / 701~1200 / 1201+
    public static int resolveLevel(int charCount) {
        if (charCount <= 0)    return 0;
        if (charCount <= 300)  return 1;
        if (charCount <= 700)  return 2;
        if (charCount <= 1200) return 3;
        return 4;
    }
}
