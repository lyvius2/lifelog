package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 검색 조건")
data class PostSearchCondition(
    @Schema(description = "검색 키워드 (제목, 내용에서 검색)", example = "Spring Boot")
    val keyword: String? = null,

    @Schema(description = "카테고리 시퀀스", example = "3")
    val categorySeq: Long? = null,

    @Schema(description = "태그명", example = "Java")
    val tag: String? = null,

    @Schema(description = "게시글 상태 (DRAFT, PUBLISHED, ARCHIVED)", example = "PUBLISHED")
    val status: String? = null,

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
    val page: Int = 1.coerceAtLeast(1),

    @Schema(description = "페이지당 게시글 수", example = "10", defaultValue = "10")
    val size: Int = 10,
) {
    companion object {
        @JvmStatic
        fun of(keyword: String? = null, categorySeq: Long? = null, tag: String? = null, page: Int = 1) = PostSearchCondition(
            keyword = keyword,
            categorySeq = categorySeq,
            tag = tag,
            page = page.coerceAtLeast(1)
        )
    }
}
