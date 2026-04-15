package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사진 목록 조회 요청")
data class PhotoSearchRequest(
    @Schema(description = "카테고리 시퀀스", example = "1")
    val categorySeq: Long? = null,

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
    val page: Int = 1.coerceAtLeast(1),

    @Schema(description = "페이지 사이즈", example = "24")
    val size: Int = 24
)
