package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사진 업로드 요청")
data class UploadRequest(
    @Schema(description = "사진 제목", example = "벚꽃이 핀 거리")
    val title: String,

    @Schema(description = "사진 설명", example = "봄날의 풍경")
    val caption: String,

    @Schema(description = "카테고리 시퀀스", example = "1")
    val categorySeq: Long,

    @Schema(description = "태그 목록", example = "[\"#봄\", \"#벚꽃\"]")
    val tags: List<String>?,

    @Schema(description = "카메라 제조사", example = "SONY")
    val maker: String?,

    @Schema(description = "카메라 모델", example = "ILCE-7M4")
    val model: String?,

    @Schema(description = "렌즈 모델", example = "FE 24-70mm F2.8 GM II")
    val lens: String?,

    @Schema(description = "조리개 값", example = "f/2.8")
    val aperture: String?,

    @Schema(description = "셔터 스피드", example = "1/250s")
    val shutter: String?,

    @Schema(description = "ISO 감도", example = "400")
    val iso: String?,

    @Schema(description = "초점 거리 (mm)", example = "50")
    val focalLength: Long?,

    @Schema(description = "플래시 발광 여부", example = "Off")
    val flash: String?,

    @Schema(description = "GPS 위도", example = "37.56653")
    val latitude: Double?,

    @Schema(description = "GPS 경도", example = "126.97797")
    val longitude: Double?,

    @Schema(description = "촬영 일시", example = "2026-03-09T14:30:00")
    val shotAt: LocalDateTime?,
)
