package com.walter.lifelog.content.facade

import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import com.walter.lifelog.content.service.StaticPageService
import com.walter.lifelog.content.util.HtmlMarkupSupporter
import com.walter.lifelog.content.util.HtmlScrapeSupporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StaticPageFacade 테스트")
class StaticPageFacadeTest {

    private val staticPageService: StaticPageService = mockk()
    private val htmlScrapeSupporter: HtmlScrapeSupporter = mockk()
    private val sut = StaticPageFacade(staticPageService, HtmlMarkupSupporter(), htmlScrapeSupporter)

    @Test
    @DisplayName("registerFromMarkup - 붙여넣은 마크업을 PASTE 출처로 등록하고 title 을 뽑는다")
    fun registerFromMarkup_validMarkup_registersAsPaste() {
        // given
        val markup = createMarkup()
        every {
            staticPageService.register(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns createResponse()

        // when
        val response = sut.registerFromMarkup("my-page", markup, 1L, overwrite = false, isPublic = true)

        // then
        assertThat(response.sourceType).isEqualTo(StaticPageSourceType.PASTE)
        verify(exactly = 1) {
            staticPageService.register(
                requestedPath = "my-page",
                sourceType = StaticPageSourceType.PASTE,
                markup = markup,
                title = "대시보드",
                sourceUrl = null,
                sourceFileName = null,
                userSeq = 1L,
                overwrite = false,
                isPublic = true,
            )
        }
    }

    @Test
    @DisplayName("registerFromMarkup - HTML 문서가 아니면 등록하지 않고 거부한다")
    fun registerFromMarkup_notHtml_throws() {
        // given
        val markup = "그냥 한참 길게 적어둔 평범한 텍스트입니다. 태그가 하나도 없습니다."

        // when & then
        assertThatThrownBy { sut.registerFromMarkup(null, markup, 1L, false, true) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("HTML 문서로 보이지 않습니다")
        verify(exactly = 0) {
            staticPageService.register(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun createMarkup(): String =
        "<!DOCTYPE html><html><head><title>대시보드</title></head><body><p>본문</p></body></html>"

    private fun createResponse(): StaticPageResponse = StaticPageResponse(
        path = "my-page",
        url = "/static/my-page",
        sourceType = StaticPageSourceType.PASTE,
        byteSize = 100,
    )
}
