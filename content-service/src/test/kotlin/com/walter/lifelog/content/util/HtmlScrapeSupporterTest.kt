package com.walter.lifelog.content.util

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 네트워크를 타기 전에 거부되는 경로만 검증합니다. 실제 응답 처리(200 확인·Content-Type
 * 확인·크기 제한)는 공개 주소가 필요하므로 단위 테스트에서 다루지 않습니다.
 */
@DisplayName("HtmlScrapeSupporter 테스트")
class HtmlScrapeSupporterTest {

    private val sut = HtmlScrapeSupporter(HtmlMarkupSupporter())

    @Test
    @DisplayName("fetchMarkup - http·https 가 아닌 scheme 은 거부한다")
    fun fetchMarkup_unsupportedScheme_throws() {
        // given
        val urls = listOf(
            "file:///etc/passwd",
            "ftp://example.com/page.html",
            "gopher://example.com/",
            "jar:file:///tmp/a.jar!/b.html",
        )

        // when & then
        urls.forEach { url ->
            assertThatThrownBy { sut.fetchMarkup(url) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("http 또는 https")
        }
    }

    @Test
    @DisplayName("fetchMarkup - 주소가 비어 있으면 거부한다")
    fun fetchMarkup_blank_throws() {
        // given & when & then
        listOf("", "   ").forEach { url ->
            assertThatThrownBy { sut.fetchMarkup(url) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("주소를 입력해주세요")
        }
    }

    @Test
    @DisplayName("fetchMarkup - 루프백·사설망·링크로컬 주소는 거부한다")
    fun fetchMarkup_internalAddress_throws() {
        // given
        val internalUrls = listOf(
            "http://127.0.0.1:8080/actuator/env",
            "http://localhost:3306/",
            "http://192.168.0.10/admin",
            "http://10.0.0.5/",
            "http://172.16.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/",
            "http://[::1]/",
        )

        // when & then
        internalUrls.forEach { url ->
            assertThatThrownBy { sut.fetchMarkup(url) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("내부망 주소는 등록할 수 없습니다")
        }
    }

    @Test
    @DisplayName("fetchMarkup - 호스트가 없는 주소는 거부한다")
    fun fetchMarkup_noHost_throws() {
        // given & when & then
        assertThatThrownBy { sut.fetchMarkup("http:///page.html") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("호스트가 없습니다")
    }
}
