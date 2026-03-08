package com.walter.lifelog.photo.dto

import java.io.InputStream

data class ImageResource(
    val inputStream: InputStream,
    val mimeType: String,
    val fileName: String,
    val fileSize: Long
) {
    fun isImage(): Boolean = mimeType.startsWith("image/")
}

