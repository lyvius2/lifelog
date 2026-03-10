package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "EXIF 정보")
data class ExifInfo(
    @Schema(description = "제조사", example = "Sony")
    val maker: String?,

    @Schema(description = "모델", example = "α7 IV")
    val model: String?,

    @Schema(description = "조리개", example = "f/8")
    val aperture: String?,

    @Schema(description = "셔터 속도", example = "1/250s")
    val shutter: String?,

    @Schema(description = "ISO", example = "100")
    val iso: String?,

    @Schema(description = "초점 거리", example = "24")
    val focal: Long?,

    @Schema(description = "렌즈", example = "FE 24-70mm F2.8 GM II")
    val lens: String?,

    @Schema(description = "위도", example = "37.7749")
    val latitude: BigDecimal?,

    @Schema(description = "경도", example = "-122.4194")
    val longitude: BigDecimal?
)
