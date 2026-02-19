package com.walter.lifelog.util;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.*;

import java.util.*;

/**
 * Markdown을 post.html 템플릿 구조에 맞는 HTML로 변환하는 유틸리티 클래스.
 *
 * <p>변환 규칙:</p>
 * <ul>
 *   <li>{@code ## 제목} → {@code <h5 id="slug">제목</h5>}</li>
 *   <li>{@code ```lang 코드```} → {@code <div class="code-wrap">} 구조 (toolbar + dots + lang label + copy 버튼)</li>
 *   <li>{@code > 인용} → {@code <div class="callout">} 구조</li>
 *   <li>나머지(p, ul, ol, a, code 등)는 표준 HTML 태그로 출력되어 post.css의 {@code .post-body} 하위 스타일이 적용됨</li>
 * </ul>
 *
 * <p>사용 예:</p>
 * <pre>{@code
 * String markdown = "## 제목\n\n본문 텍스트\n\n```java\nSystem.out.println(\"hello\");\n```";
 * String html = MarkdownConverter.convert(markdown);
 * }</pre>
 */
public final class MarkdownConverter {

    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            TaskListItemsExtension.create()
    );

    private static final Parser PARSER = Parser.builder()
            .extensions(EXTENSIONS)
            .build();

    private MarkdownConverter() {
        // 인스턴스 생성 방지
    }

    /**
     * Markdown 문자열을 post.html 템플릿 구조에 맞는 HTML로 변환한다.
     *
     * @param markdown 변환할 Markdown 문자열
     * @return post.html 호환 HTML 문자열
     */
    public static String convert(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Node document = PARSER.parse(markdown);

        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(EXTENSIONS)
                .nodeRendererFactory(PostTemplateRenderer::new)
                .build();

        return renderer.render(document).trim();
    }

    /**
     * Markdown에서 제목(Heading) 목록을 추출한다. (TOC 생성용)
     *
     * @param markdown Markdown 문자열
     * @return 제목 정보 리스트 (level, id, text)
     */
    public static List<HeadingInfo> extractHeadings(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return Collections.emptyList();
        }

        Node document = PARSER.parse(markdown);
        List<HeadingInfo> headings = new ArrayList<>();

        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                String text = extractText(heading);
                String id = slugify(text);
                headings.add(new HeadingInfo(heading.getLevel(), id, text));
            }
        });

        return headings;
    }

    /**
     * 제목(Heading) 정보를 담는 레코드.
     *
     * @param level 제목 레벨 (1~6)
     * @param id    HTML id 속성 값 (slug)
     * @param text  제목 텍스트
     */
    public record HeadingInfo(int level, String id, String text) {}

    // ─────────────────────────────────────────────────────────────
    //  post.html 템플릿 구조에 맞는 커스텀 노드 렌더러
    // ─────────────────────────────────────────────────────────────

    private static class PostTemplateRenderer implements NodeRenderer {

        private final HtmlWriter html;
        private final HtmlNodeRendererContext context;

        PostTemplateRenderer(HtmlNodeRendererContext context) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(
                    Heading.class,
                    FencedCodeBlock.class,
                    IndentedCodeBlock.class,
                    BlockQuote.class
            );
        }

        @Override
        public void render(Node node) {
            switch (node) {
                case Heading h -> renderHeading(h);
                case FencedCodeBlock cb -> renderFencedCodeBlock(cb);
                case IndentedCodeBlock ib -> renderIndentedCodeBlock(ib);
                case BlockQuote bq -> renderBlockQuote(bq);
                default -> { /* CoreHtmlNodeRenderer가 처리 */ }
            }
        }

        /**
         * ## 제목 → <h5 id="slug">제목</h5>
         * post.css에서 .post-body h5 스타일 + ::before 그라디언트 바가 자동 적용됨
         */
        private void renderHeading(Heading heading) {
            String text = extractText(heading);
            String id = slugify(text);

            // post.html에서는 섹션 제목에 h5를 사용
            html.line();
            html.tag("h5", Map.of("id", id));
            renderChildren(heading);
            html.tag("/h5");
            html.line();
        }

        /**
         * ```lang 코드블록``` → post.html의 code-wrap 구조
         * <div class="code-wrap">
         *   <div class="code-toolbar">
         *     <div class="code-toolbar-dots"><span></span><span></span><span></span></div>
         *     <span class="code-toolbar-lang">lang</span>
         *     <button class="code-copy-btn" onclick="copyCode(this)">Copy</button>
         *   </div>
         *   <pre><code>...</code></pre>
         * </div>
         */
        private void renderFencedCodeBlock(FencedCodeBlock codeBlock) {
            String lang = codeBlock.getInfo() != null ? codeBlock.getInfo().trim() : "";
            String code = codeBlock.getLiteral();

            renderCodeWrap(lang, code);
        }

        /**
         * 들여쓰기 코드블록 → code-wrap 구조 (언어 표시 없음)
         */
        private void renderIndentedCodeBlock(IndentedCodeBlock codeBlock) {
            renderCodeWrap("", codeBlock.getLiteral());
        }

        /**
         * > 인용문 → <div class="callout">내용</div>
         * post.css의 .callout 스타일이 적용됨
         */
        private void renderBlockQuote(BlockQuote blockQuote) {
            html.line();
            html.tag("div", Map.of("class", "callout"));
            html.line();
            renderChildren(blockQuote);
            html.tag("/div");
            html.line();
        }

        // ─── 공통 헬퍼 ───

        private void renderCodeWrap(String lang, String code) {
            String langDisplay = lang.isEmpty() ? "text" : lang.toLowerCase();

            // 끝에 불필요한 줄바꿈 제거
            if (code.endsWith("\n")) {
                code = code.substring(0, code.length() - 1);
            }

            html.line();
            html.tag("div", Map.of("class", "code-wrap"));
            html.line();

            // toolbar
            html.tag("div", Map.of("class", "code-toolbar"));

            // dots
            html.tag("div", Map.of("class", "code-toolbar-dots"));
            html.raw("<span></span><span></span><span></span>");
            html.tag("/div");

            // lang label
            html.tag("span", Map.of("class", "code-toolbar-lang"));
            html.text(langDisplay);
            html.tag("/span");

            // copy button
            html.raw("<button class=\"code-copy-btn\" onclick=\"copyCode(this)\">Copy</button>");

            html.tag("/div"); // /code-toolbar
            html.line();

            // pre > code
            html.tag("pre");
            if (!lang.isEmpty()) {
                html.tag("code", Map.of("class", "language-" + lang));
            } else {
                html.tag("code");
            }
            html.text(code);
            html.tag("/code");
            html.tag("/pre");
            html.line();

            html.tag("/div"); // /code-wrap
            html.line();
        }

        private void renderChildren(Node parent) {
            Node node = parent.getFirstChild();
            while (node != null) {
                Node next = node.getNext();
                context.render(node);
                node = next;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  공통 유틸 메서드
    // ─────────────────────────────────────────────────────────────

    /**
     * 노드 하위의 모든 텍스트를 추출한다.
     */
    static String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                sb.append(code.getLiteral());
            }

            @Override
            public void visit(SoftLineBreak softLineBreak) {
                sb.append(' ');
            }
        });
        return sb.toString().trim();
    }

    /**
     * 텍스트를 URL-safe slug로 변환한다.
     * 예: "Kafka Consumer 설정" → "kafka-consumer-설정"
     */
    static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s-]", "")  // 문자, 숫자, 공백, 하이픈만 유지
                .replaceAll("\\s+", "-")                   // 공백 → 하이픈
                .replaceAll("-+", "-")                      // 연속 하이픈 제거
                .replaceAll("^-|-$", "");                   // 앞뒤 하이픈 제거
    }
}

