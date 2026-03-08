package com.walter.lifelog.photo.dto

data class UploadResult(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val webViewLink: String?,
    val webContentLink: String?
)

