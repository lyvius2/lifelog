package com.walter.lifelog.photo.service

import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.photo.dto.UploadResult
import com.walter.lifelog.shared.util.GoogleDriveHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Collections

@Service
class GoogleDriveService(
    private val googleDriveHelper: GoogleDriveHelper,
) {
    fun uploadImage(folderPath: String, fileName: String, mimeType: String, inputStream: InputStream): UploadResult {
        require(mimeType.startsWith("image/")) { "이미지 파일만 업로드 가능합니다: $mimeType" }
        val drive = googleDriveHelper.drive
        val folders = folderPath.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var parentId = "root"
        for (folder in folders) {
            parentId = googleDriveHelper.findOrCreateFolder(drive, parentId, folder)
        }

        val fileMetadata = File().apply {
            name = fileName
            parents = Collections.singletonList(parentId)
        }

        val mediaContent = InputStreamContent(mimeType, inputStream)
        val uploaded = drive.files().create(fileMetadata, mediaContent)
            .setFields("id, name, mimeType, size, webViewLink, webContentLink")
            .execute()
        return UploadResult(
            fileId = uploaded.id,
            fileName = uploaded.name,
            mimeType = uploaded.mimeType,
            fileSize = uploaded.size.toLong(),
            webViewLink = uploaded.webViewLink,
            webContentLink = uploaded.webContentLink
        )
    }

    fun getImageByPath(path: String): ImageResource? {
        val drive = googleDriveHelper.drive
        val segments = path.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            throw RuntimeException("경로가 비어 있습니다: $path")
        }

        var parentId = "root"
        for (i in 0 until segments.size - 1) {
            parentId = googleDriveHelper.findFileId(drive, parentId, segments[i], true) ?: run {
                throw RuntimeException("폴더를 찾을 수 없습니다: ${segments[i]} (경로: $path)")
            }
        }

        val fileName = segments.last()
        val fileId = googleDriveHelper.findFileId(drive, parentId, fileName, false) ?: run {
            throw RuntimeException("파일을 찾을 수 없습니다: $fileName (경로: $path)")
        }

        val fileMeta = drive.files().get(fileId)
            .setFields("id, name, mimeType, size")
            .execute()
        val mimeType = fileMeta.mimeType
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw RuntimeException("파일이 이미지가 아닙니다: ${fileMeta.getName()} (경로: $path)")
        }

        val buffer = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(buffer)
        val bytes = buffer.toByteArray()
        return ImageResource(
            ByteArrayInputStream(bytes),
            mimeType,
            fileMeta.name,
            bytes.size.toLong()
        )
    }
}
