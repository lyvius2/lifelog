package com.walter.lifelog.blog.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 저장 응답 DTO")
data class PostSaveResponse(
    @Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    val title: String,
) {
    companion object {
        @JvmStatic
        fun of(postResponse: PostResponse): PostSaveResponse {
            return PostSaveResponse(postResponse.postSeq!!, postResponse.title)
        }
    }
}
