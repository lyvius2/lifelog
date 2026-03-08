package com.walter.lifelog.photo.dto

import com.google.api.services.drive.model.File

data class UploadResult(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val drivePath: String,
    val webViewLink: String?,
    val webContentLink: String?
) {
    companion object {
        fun of(driveFile: File, drivePath: String): UploadResult {
            return UploadResult(driveFile.id, driveFile.name, driveFile.mimeType, driveFile.size.toLong(), "${drivePath}/${driveFile.name}", driveFile.webViewLink, driveFile.webContentLink)
        }
    }}

