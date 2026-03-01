package com.walter.lifelog.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 저장 응답 DTO")
data class PostSaveResponse(
    @field:Schema(description = "게시글 시퀀스", example = "1")
    val postSeq: Long,

    @field:Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    val title: String,
)
