package com.walter.lifelog.content.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("HtmlMarkupSupporter 테스트")
class HtmlMarkupSupporterTest {

    private val sut = HtmlMarkupSupporter()

    @Test
    @DisplayName("validateFileName - html·htm 확장자만 통과시킨다")
    fun validateFileName_onlyHtmlExtensions() {
        // given & when & then
        assertThatCode { sut.validateFileName("page.html") }.doesNotThrowAnyException()
        assertThatCode { sut.validateFileName("page.HTM") }.doesNotThrowAnyException()
        listOf("page.txt", "page.pdf", "page", null, "page.html.exe").forEach { fileName ->
            assertThatThrownBy { sut.validateFileName(fileName) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("HTML 파일만")
        }
    }

    @Test
    @DisplayName("toValidatedMarkup - 문서 골격을 갖춘 UTF-8 바이트면 마크업을 돌려준다")
    fun toValidatedMarkup_validDocument_returnsMarkup() {
        // given
        val markup = "<!DOCTYPE html><html><body><p>한글도 됩니다</p></body></html>"

        // when
        val result = sut.toValidatedMarkup(markup.toByteArray(Charsets.UTF_8))

        // then
        assertThat(result).isEqualTo(markup)
    }

    @Test
    @DisplayName("toValidatedMarkup - html·body·doctype 중 아무것도 없으면 거부한다")
    fun toValidatedMarkup_noDocumentMarker_throws() {
        // given
        val notHtml = "이것은 그냥 텍스트입니다. 태그가 전혀 없습니다. 길이는 충분합니다."

        // when & then
        assertThatThrownBy { sut.toValidatedMarkup(notHtml.toByteArray(Charsets.UTF_8)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("HTML 문서로 보이지 않습니다")
    }

    @Test
    @DisplayName("toValidatedMarkup - 내용이 너무 짧으면 거부한다")
    fun toValidatedMarkup_tooShort_throws() {
        // given
        val tooShort = "<html></html>"

        // when & then
        assertThatThrownBy { sut.toValidatedMarkup(tooShort.toByteArray(Charsets.UTF_8)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("너무 짧습니다")
    }

    @Test
    @DisplayName("toValidatedMarkup - 최대 크기를 넘으면 거부한다")
    fun toValidatedMarkup_tooLarge_throws() {
        // given
        val oversized = ("<html><body>" + "a".repeat(HtmlMarkupSupporter.MAX_BYTES)).toByteArray(Charsets.UTF_8)

        // when & then
        assertThatThrownBy { sut.toValidatedMarkup(oversized) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("너무 큽니다")
    }

    @Test
    @DisplayName("toValidatedMarkup - NUL 바이트가 섞인 바이너리는 거부한다")
    fun toValidatedMarkup_binary_throws() {
        // given
        val binary = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x00) + ByteArray(50)

        // when & then
        assertThatThrownBy { sut.toValidatedMarkup(binary) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("텍스트가 아닌 파일입니다")
    }

    @Test
    @DisplayName("extractTitle - title 태그가 있으면 공백을 정리해 뽑아낸다")
    fun extractTitle_present_returnsTrimmedTitle() {
        // given
        val markup = "<html><head><title>  Sales\n  Dashboard  </title></head><body>x</body></html>"

        // when
        val result = sut.extractTitle(markup)

        // then
        assertThat(result).isEqualTo("Sales Dashboard")
    }

    @Test
    @DisplayName("extractTitle - title 이 없거나 비어 있으면 null 을 돌려준다")
    fun extractTitle_absentOrBlank_returnsNull() {
        // given & when & then
        assertThat(sut.extractTitle("<html><body>x</body></html>")).isNull()
        assertThat(sut.extractTitle("<html><head><title>   </title></head></html>")).isNull()
    }
}
