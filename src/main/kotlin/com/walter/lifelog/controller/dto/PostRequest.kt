package com.walter.lifelog.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 등록 요청 DTO")
data class PostRequest(
    @field:Schema(description = "작성자 시퀀스", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val userSeq: Long,

    @field:Schema(description = "카테고리 시퀀스", example = "3")
    val categorySeq: Long? = null,

    @field:Schema(description = "게시글 제목", example = "Spring Boot 시작하기", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String,

    @field:Schema(description = "게시글 slug (URL 경로)", example = "spring-boot-getting-started")
    val slug: String? = null,

    @field:Schema(description = "게시글 요약", example = "Spring Boot를 시작하는 방법을 알아봅니다.")
    val summary: String? = null,

    @field:Schema(description = "게시글 HTML 내용", example = "<h5>소개</h5><p>본문 내용</p>", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,

    @field:Schema(description = "게시글 Markdown 원본 내용", example = "## 소개\n\n본문 내용")
    val markdownContent: String? = null,

    @field:Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    val thumbnailUrl: String? = null,

    @field:Schema(description = "게시글 상태 (DRAFT, PUBLISHED, ARCHIVED)", example = "DRAFT")
    val status: String? = null,

    @field:Schema(description = "추천 게시글 여부", example = "false")
    val isFeatured: Boolean = false,

    @field:Schema(description = "태그 목록", example = "[\"Spring\", \"Java\", \"Backend\"]")
    val tags: List<String>? = null
)