package com.walter.lifelog.content.dto

import com.walter.lifelog.content.entity.StaticPage
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "정적 웹페이지 등록 결과")
data class StaticPageResponse(
    @Schema(description = "접속 경로", example = "a7f3k9d2")
    val path: String,

    @Schema(description = "전체 접속 주소", example = "/static/a7f3k9d2")
    val url: String,

    @Schema(description = "등록 방식", example = "URL")
    val sourceType: StaticPageSourceType,

    @Schema(description = "스크랩 원본 주소", example = "https://claude.ai/public/artifacts/xxxx")
    val sourceUrl: String? = null,

    @Schema(description = "업로드한 파일명", example = "dashboard.html")
    val sourceFileName: String? = null,

    @Schema(description = "문서 title", example = "Sales Dashboard")
    val title: String? = null,

    @Schema(description = "마크업 바이트 크기", example = "48213")
    val byteSize: Int,

    @Schema(description = "등록 시각")
    val createdAt: LocalDateTime? = null,
) {
    companion object {
        fun of(staticPage: StaticPage) = StaticPageResponse(
            path = staticPage.path,
            url = "$URL_PREFIX${staticPage.path}",
            sourceType = staticPage.sourceType,
            sourceUrl = staticPage.sourceUrl,
            sourceFileName = staticPage.sourceFileName,
            title = staticPage.title,
            byteSize = staticPage.byteSize,
            createdAt = staticPage.createdAt,
        )

        const val URL_PREFIX = "/static/"
    }
}
