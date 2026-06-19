package com.Rootin.domain.til.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TilContentLength: TIL 본문 HTML에서 경험치 산정용 글자 수 계산")
class TilContentLengthTest {

    @Test
    @DisplayName("HTML 태그는 글자 수에서 제외하고 보이는 텍스트만 센다")
    void countsOnlyVisibleText() {
        assertThat(TilContentLength.countVisibleCharacters("<p>가나다</p>")).isEqualTo(3);
    }

    @Test
    @DisplayName("공백·개행은 글자 수에서 제외한다 (FE getText().replace(/\\s/g,'') 기준과 일치)")
    void stripsWhitespace() {
        assertThat(TilContentLength.countVisibleCharacters("<p>가 나\n다</p>")).isEqualTo(3);
    }

    @Test
    @DisplayName("HTML 엔티티는 디코딩한 실제 글자 1자로 센다")
    void decodesHtmlEntities() {
        // 화면에 보이는 글자는 "a<b&c" → 공백 제외 5자
        assertThat(TilContentLength.countVisibleCharacters("<code>a &lt; b &amp; c</code>")).isEqualTo(5);
    }

    @Test
    @DisplayName("같은 텍스트면 굵게·콜아웃·코드블록 등 서식을 넣어도 글자 수가 같다")
    void formattingDoesNotInflateCount() {
        int plain = TilContentLength.countVisibleCharacters("<p>오늘배운것</p>");
        int formatted = TilContentLength.countVisibleCharacters(
                "<div data-type=\"callout\"><p><strong>오늘</strong><code>배운</code>것</p></div>");
        assertThat(formatted).isEqualTo(plain).isEqualTo(5);
    }

    @Test
    @DisplayName("null 또는 빈 문자열은 0을 반환한다")
    void returnsZeroForEmpty() {
        assertThat(TilContentLength.countVisibleCharacters(null)).isZero();
        assertThat(TilContentLength.countVisibleCharacters("")).isZero();
        assertThat(TilContentLength.countVisibleCharacters("   ")).isZero();
    }
}
