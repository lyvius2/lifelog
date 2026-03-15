package com.walter.lifelog.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarkdownConverter 테스트")
class MarkdownConverterTest {

    @Nested
    @DisplayName("convert() 메서드")
    class Convert {

        @Test
        @DisplayName("null 또는 빈 문자열을 전달하면 빈 문자열을 반환한다")
        void shouldReturnEmptyForNullOrBlank() {
            assertThat(MarkdownConverter.convert(null)).isEmpty();
            assertThat(MarkdownConverter.convert("")).isEmpty();
            assertThat(MarkdownConverter.convert("   ")).isEmpty();
        }

        @Test
        @DisplayName("일반 텍스트를 <p> 태그로 감싼다")
        void shouldWrapPlainTextInParagraph() {
            String result = MarkdownConverter.convert("Hello, World!");
            assertThat(result).contains("<p>Hello, World!</p>");
        }

        @Test
        @DisplayName("# H1 → <h3> 태그로 변환한다")
        void shouldConvertH1ToH3() {
            String result = MarkdownConverter.convert("# 대제목");
            assertThat(result).contains("<h3 id=\"대제목\">대제목</h3>");
        }

        @Test
        @DisplayName("## H2 → <h5> 태그로 변환한다")
        void shouldConvertH2ToH5() {
            String result = MarkdownConverter.convert("## 섹션 제목");
            assertThat(result).contains("<h5 id=\"섹션-제목\">섹션 제목</h5>");
        }

        @Test
        @DisplayName("### H3 → <h6> 태그로 변환한다")
        void shouldConvertH3ToH6() {
            String result = MarkdownConverter.convert("### 소제목");
            assertThat(result).contains("<h6 id=\"소제목\">소제목</h6>");
        }

        @Test
        @DisplayName("코드 블록을 code-wrap 구조로 변환한다")
        void shouldConvertFencedCodeBlockToCodeWrap() {
            String markdown = """
                    ```java
                    System.out.println("hello");
                    ```
                    """;
            String result = MarkdownConverter.convert(markdown);

            assertThat(result).contains("<div class=\"code-wrap\">");
            assertThat(result).contains("<div class=\"code-toolbar\">");
            assertThat(result).contains("<div class=\"code-toolbar-dots\">");
            assertThat(result).contains("<span class=\"code-toolbar-lang\">java</span>");
            assertThat(result).contains("<button class=\"code-copy-btn\"");
            assertThat(result).contains("Copy</button>");
            assertThat(result).contains("<pre><code class=\"language-java\">");
            assertThat(result).contains("System.out.println(&quot;hello&quot;);");
        }

        @Test
        @DisplayName("언어 미지정 코드 블록은 'text'로 표시한다")
        void shouldDisplayTextForNoLanguageCodeBlock() {
            String markdown = """
                    ```
                    some code
                    ```
                    """;
            String result = MarkdownConverter.convert(markdown);

            assertThat(result).contains("<span class=\"code-toolbar-lang\">text</span>");
            assertThat(result).contains("<pre><code>");
        }

        @Test
        @DisplayName("인용문을 callout div로 변환한다")
        void shouldConvertBlockQuoteToCallout() {
            String result = MarkdownConverter.convert("> 이것은 인용문입니다.");
            assertThat(result).contains("<div class=\"callout\">");
            assertThat(result).contains("이것은 인용문입니다.");
        }

        @Test
        @DisplayName("이미지를 figure 구조로 변환한다")
        void shouldConvertImageToFigure() {
            String result = MarkdownConverter.convert("![대체 텍스트](https://example.com/img.jpg)");
            assertThat(result).contains("<figure class=\"post-image\">");
            assertThat(result).contains("src=\"https://example.com/img.jpg\"");
            assertThat(result).contains("alt=\"대체 텍스트\"");
            assertThat(result).contains("loading=\"lazy\"");
            assertThat(result).contains("<figcaption>대체 텍스트</figcaption>");
        }

        @Test
        @DisplayName("수평선을 post-divider로 변환한다")
        void shouldConvertThematicBreakToPostDivider() {
            String result = MarkdownConverter.convert("---");
            assertThat(result).contains("<hr class=\"post-divider\" />");
        }

        @Test
        @DisplayName("볼드, 이탤릭 등 인라인 서식을 변환한다")
        void shouldConvertInlineFormatting() {
            String result = MarkdownConverter.convert("**볼드** 그리고 *이탤릭*");
            assertThat(result).contains("<strong>볼드</strong>");
            assertThat(result).contains("<em>이탤릭</em>");
        }

        @Test
        @DisplayName("인라인 코드를 <code> 태그로 변환한다")
        void shouldConvertInlineCode() {
            String result = MarkdownConverter.convert("이것은 `inline code` 입니다.");
            assertThat(result).contains("<code>inline code</code>");
        }

        @Test
        @DisplayName("링크를 <a> 태그로 변환한다")
        void shouldConvertLink() {
            String result = MarkdownConverter.convert("[구글](https://www.google.com)");
            assertThat(result).contains("<a href=\"https://www.google.com\">구글</a>");
        }

        @Test
        @DisplayName("순서 있는 리스트를 <ol> 태그로 변환한다")
        void shouldConvertOrderedList() {
            String markdown = """
                    1. 첫 번째
                    2. 두 번째
                    3. 세 번째
                    """;
            String result = MarkdownConverter.convert(markdown);
            assertThat(result).contains("<ol>");
            assertThat(result).contains("<li>첫 번째</li>");
            assertThat(result).contains("<li>두 번째</li>");
            assertThat(result).contains("<li>세 번째</li>");
        }

        @Test
        @DisplayName("순서 없는 리스트를 <ul> 태그로 변환한다")
        void shouldConvertUnorderedList() {
            String markdown = """
                    - 항목 A
                    - 항목 B
                    - 항목 C
                    """;
            String result = MarkdownConverter.convert(markdown);
            assertThat(result).contains("<ul>");
            assertThat(result).contains("<li>항목 A</li>");
            assertThat(result).contains("<li>항목 B</li>");
        }

        @Test
        @DisplayName("GFM 테이블을 <table> 태그로 변환한다")
        void shouldConvertGfmTable() {
            String markdown = """
                    | 항목 | 설명 |
                    |------|------|
                    | A    | 첫째 |
                    | B    | 둘째 |
                    """;
            String result = MarkdownConverter.convert(markdown);
            assertThat(result).contains("<table>");
            assertThat(result).contains("<th>항목</th>");
            assertThat(result).contains("<td>A</td>");
        }

        @Test
        @DisplayName("취소선을 <del> 태그로 변환한다")
        void shouldConvertStrikethrough() {
            String result = MarkdownConverter.convert("~~취소선~~");
            assertThat(result).contains("<del>취소선</del>");
        }

        @Test
        @DisplayName("복합 마크다운을 올바르게 변환한다")
        void shouldConvertComplexMarkdown() {
            String markdown = """
                    # 제목
                    
                    본문 단락입니다.
                    
                    ## 코드 예시
                    
                    ```kotlin
                    fun main() {
                        println("Hello")
                    }
                    ```
                    
                    > 참고 사항입니다.
                    
                    ---
                    
                    ### 마무리
                    
                    감사합니다.
                    """;
            String result = MarkdownConverter.convert(markdown);

            assertThat(result).contains("<h3 id=\"제목\">제목</h3>");
            assertThat(result).contains("<p>본문 단락입니다.</p>");
            assertThat(result).contains("<h5 id=\"코드-예시\">코드 예시</h5>");
            assertThat(result).contains("<div class=\"code-wrap\">");
            assertThat(result).contains("<span class=\"code-toolbar-lang\">kotlin</span>");
            assertThat(result).contains("<div class=\"callout\">");
            assertThat(result).contains("<hr class=\"post-divider\" />");
            assertThat(result).contains("<h6 id=\"마무리\">마무리</h6>");
            assertThat(result).contains("<p>감사합니다.</p>");
        }
    }

    @Nested
    @DisplayName("extractHeadings() 메서드")
    class ExtractHeadings {

        @Test
        @DisplayName("null 또는 빈 문자열을 전달하면 빈 리스트를 반환한다")
        void shouldReturnEmptyListForNullOrBlank() {
            assertThat(MarkdownConverter.extractHeadings(null)).isEmpty();
            assertThat(MarkdownConverter.extractHeadings("")).isEmpty();
        }

        @Test
        @DisplayName("마크다운에서 제목 목록을 올바르게 추출한다")
        void shouldExtractHeadingsCorrectly() {
            String markdown = """
                    # 대제목
                    
                    본문
                    
                    ## 섹션 1
                    
                    내용
                    
                    ### 소제목 A
                    
                    ## 섹션 2
                    """;
            List<MarkdownConverter.HeadingInfo> headings = MarkdownConverter.extractHeadings(markdown);

            assertThat(headings).hasSize(4);

            assertThat(headings.get(0).level()).isEqualTo(1);
            assertThat(headings.get(0).text()).isEqualTo("대제목");
            assertThat(headings.get(0).id()).isEqualTo("대제목");

            assertThat(headings.get(1).level()).isEqualTo(2);
            assertThat(headings.get(1).text()).isEqualTo("섹션 1");
            assertThat(headings.get(1).id()).isEqualTo("섹션-1");

            assertThat(headings.get(2).level()).isEqualTo(3);
            assertThat(headings.get(2).text()).isEqualTo("소제목 A");

            assertThat(headings.get(3).level()).isEqualTo(2);
            assertThat(headings.get(3).text()).isEqualTo("섹션 2");
        }

        @Test
        @DisplayName("제목이 없는 마크다운에서는 빈 리스트를 반환한다")
        void shouldReturnEmptyListWhenNoHeadings() {
            String markdown = """
                    본문만 있는 글입니다.
                    
                    > 인용문도 있고
                    
                    - 리스트도 있지만
                    
                    제목은 없습니다.
                    """;
            List<MarkdownConverter.HeadingInfo> headings = MarkdownConverter.extractHeadings(markdown);
            assertThat(headings).isEmpty();
        }
    }

    @Nested
    @DisplayName("slugify() 메서드")
    class Slugify {

        @Test
        @DisplayName("영문과 공백을 하이픈으로 변환한다")
        void shouldConvertSpacesToHyphens() {
            assertThat(MarkdownConverter.slugify("Kafka Consumer 설정")).isEqualTo("kafka-consumer-설정");
        }

        @Test
        @DisplayName("특수문자를 제거한다")
        void shouldRemoveSpecialCharacters() {
            assertThat(MarkdownConverter.slugify("Hello, World!")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("연속된 공백과 하이픈을 하나로 합친다")
        void shouldCollapseMultipleHyphens() {
            assertThat(MarkdownConverter.slugify("a   b---c")).isEqualTo("a-b-c");
        }

        @Test
        @DisplayName("null 또는 빈 문자열은 빈 문자열을 반환한다")
        void shouldReturnEmptyForNullOrBlank() {
            assertThat(MarkdownConverter.slugify(null)).isEmpty();
            assertThat(MarkdownConverter.slugify("")).isEmpty();
            assertThat(MarkdownConverter.slugify("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractText() 메서드")
    class ExtractText {

        @Test
        @DisplayName("Heading 노드에서 텍스트를 올바르게 추출한다")
        void shouldExtractTextFromHeading() {
            // extractHeadings가 내부적으로 extractText를 사용하므로 간접 검증
            String markdown = "## Spring Boot 가이드";
            List<MarkdownConverter.HeadingInfo> headings = MarkdownConverter.extractHeadings(markdown);

            assertThat(headings).hasSize(1);
            assertThat(headings.getFirst().text()).isEqualTo("Spring Boot 가이드");
        }
    }
}

