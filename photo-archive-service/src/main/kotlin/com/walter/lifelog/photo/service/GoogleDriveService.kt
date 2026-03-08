package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.shared.util.GoogleDriveHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class GoogleDriveService(
    private val googleDriveHelper: GoogleDriveHelper,
) {
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
