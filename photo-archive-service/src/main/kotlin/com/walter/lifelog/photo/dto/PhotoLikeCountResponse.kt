package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사진 좋아요 수 응답")
data class PhotoLikeCountResponse(
    @Schema(description = "사진 시퀀스", example = "1")
    val photoSeq: Long,
    @Schema(description = "좋아요 수", example = "42")
    val likeCount: Int,
)
