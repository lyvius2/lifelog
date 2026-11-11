package com.walter.lifelog.content.facade

import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import com.walter.lifelog.content.service.StaticPageService
import com.walter.lifelog.content.util.HtmlMarkupSupporter
import com.walter.lifelog.content.util.HtmlScrapeSupporter
import com.walter.lifelog.shared.annotation.Facade

@Facade
class StaticPageFacade(
    private val staticPageService: StaticPageService,
    private val htmlMarkupSupporter: HtmlMarkupSupporter,
    private val htmlScrapeSupporter: HtmlScrapeSupporter,
) {
    fun registerFromFile(
        requestedPath: String?,
        fileName: String?,
        bytes: ByteArray,
        userSeq: Long,
    ): StaticPageResponse {
        htmlMarkupSupporter.validateFileName(fileName)
        val markup = htmlMarkupSupporter.toValidatedMarkup(bytes)
        return staticPageService.register(
            requestedPath = requestedPath,
            sourceType = StaticPageSourceType.FILE,
            markup = markup,
            title = htmlMarkupSupporter.extractTitle(markup),
            sourceUrl = null,
            sourceFileName = fileName,
            userSeq = userSeq,
        )
    }

    fun registerFromUrl(requestedPath: String?, url: String, userSeq: Long): StaticPageResponse {
        val markup = htmlScrapeSupporter.fetchMarkup(url)
        return staticPageService.register(
            requestedPath = requestedPath,
            sourceType = StaticPageSourceType.URL,
            markup = markup,
            title = htmlMarkupSupporter.extractTitle(markup),
            sourceUrl = url.trim(),
            sourceFileName = null,
            userSeq = userSeq,
        )
    }

    fun getPages(): List<StaticPageResponse> = staticPageService.getPages()

    fun delete(path: String) = staticPageService.delete(path)
}
