package com.walter.lifelog.photo.service

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.photo.util.GoogleDriveCacheSaver
import com.walter.lifelog.shared.config.exception.GoogleDriveException
import com.walter.lifelog.shared.util.GoogleDriveHelper
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class GoogleDriveService(
    private val virtualThreadExecutor: TaskExecutor,
    private val googleDriveHelper: GoogleDriveHelper,
    private val googleDriveCacheSaver: GoogleDriveCacheSaver,
) {
    fun uploadImage(folderPath: String, file: MultipartFile): Pair<File, File> {
        require(!file.isEmpty) { "file is empty" }
        val contentType = file.contentType
        require(contentType != null && contentType.startsWith("image/")) { "Only image files can be uploaded : $contentType" }

        val drive = googleDriveHelper.drive
        val folders = folderPath.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var parentId = "root"
        val pathBuilder = StringBuilder()
        for (folder in folders) {
            if (pathBuilder.isNotEmpty()) {
                pathBuilder.append("/")
            }
            pathBuilder.append(folder)
            parentId = googleDriveCacheSaver.getOrCreateFolderId(pathBuilder.toString(), drive, parentId, folder)
        }
        val fileBytes = file.bytes
        val mainJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.uploadFile(drive, file.originalFilename, parentId, ByteArrayInputStream(fileBytes), contentType)
        }
        val subJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.generateThumbnail(drive, file.originalFilename, parentId, ByteArrayInputStream(fileBytes), contentType)
        }
        val result = Pair(mainJobFuture.get(), subJobFuture.get())
        googleDriveCacheSaver.evictAllFileIdCache()
        return result
    }

    fun getImageByPath(path: String): ImageResource? {
        val segments = path.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            throw GoogleDriveException("path is empty : $path")
        }

        val drive = googleDriveHelper.drive
        val parentId = resolveFolderPath(segments.dropLast(1))
        val fileName = segments.last()
        val fileId = googleDriveCacheSaver.getFileId(path, drive, parentId, fileName)

        val mimeType = resolveMimeType(fileName)
            ?: throw GoogleDriveException("file is not image : $fileName (path: $path)")

        val buffer = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(buffer)
        val bytes = buffer.toByteArray()

        return ImageResource(
            inputStream = ByteArrayInputStream(bytes),
            mimeType = mimeType,
            fileName = fileName,
            fileSize = bytes.size.toLong()
        )
    }

    private fun resolveMimeType(fileName: String): String? {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "webp"        -> "image/webp"
            "gif"         -> "image/gif"
            "heic"        -> "image/heic"
            else          -> null
        }
    }

    private fun resolveFolderPath(folders: List<String>): String {
        if (folders.isEmpty()) {
            return "root"
        }
        val drive = googleDriveHelper.drive
        var parentId = "root"
        val pathBuilder = StringBuilder()
        for (folder in folders) {
            if (pathBuilder.isNotEmpty()) {
                pathBuilder.append("/")
            }
            pathBuilder.append(folder)
            parentId = googleDriveCacheSaver.getFolderId(pathBuilder.toString(), drive, parentId, folder)
        }
        return parentId
    }
}
