package com.Rootin.domain.til.service;

import org.jsoup.Jsoup;

/**
 * TIL 본문(HTML)에서 경험치 산정에 사용할 글자 수를 계산하는 유틸리티입니다.
 *
 * 본문은 에디터의 getHTML() 결과라 &lt;p&gt;, &lt;strong&gt;, 콜아웃/코드블록 같은 태그와 속성이 포함됩니다.
 * 이를 그대로 세면 같은 분량이라도 서식을 넣을수록 글자 수가 부풀려져 경험치가 과다 지급됩니다.
 * 그래서 태그를 제거하고 보이는 텍스트만 추출한 뒤, 공백까지 제거하여 순수 글자 수를 셉니다.
 * (프론트 예상 경험치 계산의 editor.getText().replace(/\s/g, '') 기준과 동일하게 맞춥니다.)
 */
public final class TilContentLength {

    private TilContentLength() {
    }

    /**
     * HTML 본문에서 태그·공백을 제외한 순수 보이는 글자 수를 계산합니다.
     *
     * @param html TIL 본문 HTML (null 허용)
     * @return 태그 제거 + HTML 엔티티 디코딩 + 공백 제거 후의 글자 수 (입력이 비어 있으면 0)
     */
    public static int countVisibleCharacters(String html) {
        if (html == null || html.isBlank()) {
            return 0;
        }
        // Jsoup.text()가 태그를 제거하고 HTML 엔티티(&lt; 등)를 실제 문자로 디코딩한다.
        String visibleText = Jsoup.parse(html).text();
        return visibleText.replaceAll("\\s", "").length();
    }
}
