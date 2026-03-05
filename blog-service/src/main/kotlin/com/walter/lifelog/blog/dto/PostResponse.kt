package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "게시글 응답 DTO")
data class PostResponse(
    @Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long? = null,

    @Schema(description = "카테고리 시퀀스", example = "3")
    val categorySeq: Long? = null,

    @Schema(description = "카테고리명", example = "Spring")
    val categoryName: String? = null,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    val title: String,

    @Schema(description = "게시글 요약", example = "Spring Boot를 시작하는 방법을 알아봅니다.")
    val summary: String? = null,

    @Schema(description = "게시글 HTML 내용", example = "<h5>소개</h5><p>본문 내용</p>", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,

    @Schema(description = "게시글 Markdown 원본 내용", example = "## 소개\n\n본문 내용")
    val markdownContent: String? = null,

    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    val thumbnailUrl: String? = null,

    @Schema(description = "조회수", example = "100")
    val viewCount: Int = 0,

    @Schema(description = "발행일시", example = "2026-02-15T10:30:00")
    val publishedAt: LocalDateTime? = null,

    @Schema(description = "작성자 시퀀스", example = "1")
    val userSeq: Long? = null,

    @Schema(description = "생성일시", example = "2026-02-10T09:00:00")
    val createdAt: LocalDateTime? = null,

    @Schema(description = "수정일시", example = "2026-02-17T14:20:00")
    val updatedAt: LocalDateTime? = null,

    @Schema(description = "태그 목록", example = "[\"Spring\", \"Java\", \"Backend\"]")
    var tags: List<String>? = null
) {
    companion object {
        @JvmStatic
        fun empty(): PostResponse {
            return PostResponse(title = "", content = "")
        }
    }
}