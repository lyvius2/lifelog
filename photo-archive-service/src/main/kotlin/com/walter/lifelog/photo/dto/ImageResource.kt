package com.walter.lifelog.photo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.InputStream

@Schema(description = "이미지 리소스")
data class ImageResource(
    @Schema(description = "이미지 InputStream", hidden = true)
    val inputStream: InputStream,

    @Schema(description = "MIME 타입", example = "image/jpeg")
    val mimeType: String,

    @Schema(description = "파일 이름", example = "photo.jpg")
    val fileName: String,

    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    val fileSize: Long
) {
    fun isImage(): Boolean = mimeType.startsWith("image/")
}

