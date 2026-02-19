package com.walter.lifelog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarkdownConverter 테스트")
class MarkdownConverterTest {

    @Test
    @DisplayName("Java 코드블록이 post.html의 code-wrap 구조로 변환된다")
    void convertJavaCodeBlock_shouldRenderPostTemplateCodeWrap() {
        // given
        String markdown = """
                ## Kafka Consumer
                
                Listener 역할을 할 클래스를 만든다.
                
                ```java
                @Component
                public class KafkaConsume {
                
                    @KafkaListener(topics = "TEST-TOPIC")
                    public void listener(ConsumerRecord<String, String> payload) {
                        System.out.println("Received: " + payload.value());
                    }
                }
                ```
                """;

        // when
        String html = MarkdownConverter.convert(markdown);

        // then
        // 1) h5 태그로 변환 (post.css .post-body h5 스타일 적용)
        assertThat(html).contains("<h5 id=\"kafka-consumer\">Kafka Consumer</h5>");

        // 2) code-wrap 구조가 존재
        assertThat(html).contains("<div class=\"code-wrap\">");

        // 3) toolbar에 dots 3개가 존재
        assertThat(html).contains("<div class=\"code-toolbar-dots\"><span></span><span></span><span></span></div>");

        // 4) 언어 라벨이 java로 표시
        assertThat(html).contains("<span class=\"code-toolbar-lang\">java</span>");

        // 5) Copy 버튼이 존재
        assertThat(html).contains("<button class=\"code-copy-btn\" onclick=\"copyCode(this)\">Copy</button>");

        // 6) pre > code 태그에 language 클래스가 적용
        assertThat(html).contains("<code class=\"language-java\">");

        // 7) 실제 Java 코드 내용이 포함
        assertThat(html).contains("@Component");
        assertThat(html).contains("KafkaConsume");
        assertThat(html).contains("@KafkaListener");

        // 8) 본문 단락이 p 태그로 출력 (post.css .post-body p 스타일 적용)
        assertThat(html).contains("<p>Listener 역할을 할 클래스를 만든다.</p>");
    }
}

