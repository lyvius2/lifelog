package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사진 상세 조회 응답")
data class PhotoSearchResponse(
    @Schema(description = "사진 시퀀스", example = "1")
    val photoSeq: Long,

    @Schema(description = "원본 이미지 URL")
    val src: String,

    @Schema(description = "썸네일 이미지 URL")
    val thumb: String?,

    @Schema(description = "제목", example = "礼文島 — 레분섬의 아침")
    val title: String,

    @Schema(description = "설명")
    val caption: String?,

    @Schema(description = "카테고리 시퀀스")
    val categorySeq: Long?,

    @Schema(description = "카테고리명")
    val categoryName: String?,

    @Schema(description = "태그 목록", example = "[\"#Hokkaido\", \"#Mountain\"]")
    val tags: List<String>,

    @Schema(description = "좋아요 수", example = "214")
    val likes: Int,

    @Schema(description = "촬영일시")
    val shotAt: LocalDateTime?,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "EXIF 정보")
    val exif: ExifInfo?,

    @Schema(description = "업로더 사용자 시퀀스", example = "1")
    val userSeq: Long,

    @Schema(description = "사진가 이름", example = "Walter")
    val photographerName: String?
)