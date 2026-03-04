package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 등록 요청 DTO")
data class PostRequest(
    @Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long? = null,
    
    @Schema(description = "작성자 시퀀스", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    var userSeq: Long? = null,

    @Schema(description = "카테고리 시퀀스", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    val categorySeq: Long,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String,

    @Schema(description = "게시글 slug (URL 경로)", example = "spring-boot-getting-started")
    var slug: String? = null,

    @Schema(description = "게시글 요약", example = "Spring Boot를 시작하는 방법을 알아봅니다.")
    val summary: String,

    @Schema(description = "게시글 HTML 내용", example = "<h5>소개</h5><p>본문 내용</p>")
    var content: String? = null,

    @Schema(description = "게시글 Markdown 원본 내용", example = "## 소개\n\n본문 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    val markdownContent: String? = null,

    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    val thumbnailUrl: String? = null,

    @Schema(description = "게시글 상태 (DRAFT, PUBLISHED, ARCHIVED)", example = "DRAFT", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: String,

    @Schema(description = "태그 목록", example = "[\"Spring\", \"Java\", \"Backend\"]")
    val tags: List<String>? = null
)