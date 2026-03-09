package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포토 카테고리 응답")
data class PhotoCategoryResponse(
    @Schema(description = "카테고리 시퀀스", example = "1")
    val categorySeq: Long,

    @Schema(description = "카테고리 아이콘", example = "🚗")
    val icon: String,

    @Schema(description = "카테고리 이름", example = "My Car")
    val name: String,
)
