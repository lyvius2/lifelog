package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "게시글 목록 응답 DTO")
data class PostListResponse(
    @Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    val title: String,

    @Schema(description = "게시글 요약", example = "Spring Boot를 시작하는 방법을 알아봅니다.")
    val summary: String?,

    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    val thumbnailUrl: String?,

    @Schema(description = "카테고리명", example = "Spring")
    val categoryName: String?,

    @Schema(description = "게시글 상태", example = "PUBLISHED")
    val status: String,

    @Schema(description = "조회수", example = "150")
    val viewCount: Int,

    @Schema(description = "태그 목록", example = "[\"Spring\", \"Java\"]")
    val tags: List<String> = emptyList(),

    @Schema(description = "발행일시", example = "2026-02-15T10:30:00")
    val publishedAt: LocalDateTime?,

    @Schema(description = "생성일시", example = "2026-02-10T09:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "작성자명", example = "walter")
    val writerName: String?,

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val writerProfileImage: String?,
)
