package com.walter.lifelog.photo.dto

import com.google.api.services.drive.model.File
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사진 업로드 결과")
data class UploadResponse(
    @Schema(description = "Google Drive 파일 ID", example = "1aBcDeFgHiJkLmNoPqRsT")
    val fileId: String,

    @Schema(description = "파일 이름", example = "cherry_blossom.jpg")
    val fileName: String,

    @Schema(description = "MIME 타입", example = "image/jpeg")
    val mimeType: String,

    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    val fileSize: Long,

    @Schema(description = "Drive 저장 경로", example = "lifelog/photos/nature/cherry_blossom.jpg")
    val drivePath: String,

    @Schema(description = "웹 보기 링크")
    val webViewLink: String?,

    @Schema(description = "웹 콘텐츠 다운로드 링크")
    val webContentLink: String?
) {
    companion object {
        fun of(driveFile: File, drivePath: String): UploadResponse {
            return UploadResponse(driveFile.id, driveFile.name, driveFile.mimeType, driveFile.size.toLong(), "${drivePath}/${driveFile.name}", driveFile.webViewLink, driveFile.webContentLink)
        }
    }
}

