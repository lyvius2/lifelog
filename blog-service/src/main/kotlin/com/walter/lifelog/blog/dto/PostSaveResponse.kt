package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "게시글 저장 응답 DTO")
data class PostSaveResponse(
    @Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    val title: String,

    @Schema(description = "게시글 생성 시각", example = "2024-06-01T12:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "게시글 수정 시각", example = "2024-06-01T12:00:00")
    val updatedAt: LocalDateTime?,

    @Schema(description = "게시글 게시 시각", example = "2024-06-01T12:00:00")
    val publishedAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun of(postResponse: PostResponse): PostSaveResponse {
            return PostSaveResponse(postResponse.postSeq!!, postResponse.title, postResponse.createdAt, postResponse.updatedAt, postResponse.publishedAt)
        }
    }
}
