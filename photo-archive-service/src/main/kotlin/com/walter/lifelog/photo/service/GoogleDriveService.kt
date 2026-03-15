package com.walter.lifelog.photo.service

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.photo.util.GoogleDriveCacheSaver
import com.walter.lifelog.shared.util.GoogleDriveHelper
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    fun uploadImage(folderPath: String, file: MultipartFile): Pair<File, File> {
        require(!file.isEmpty) { "file is empty" }
        val contentType = file.contentType
        require(contentType != null && contentType.startsWith("image/")) { "Only image files can be uploaded : $contentType" }

        val drive = googleDriveHelper.drive
        val folders = folderPath.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var parentId = "root"
        for (folder in folders) {
            parentId = googleDriveHelper.findOrCreateFolder(drive, parentId, folder)
        }
        val mainJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.uploadFile(file.originalFilename, parentId, file.inputStream, contentType)
        }
        val subJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.generateThumbnail(file.originalFilename, parentId, file.inputStream, contentType)
        }
        googleDriveCacheSaver.evictAllFileIdCache()
        return Pair(mainJobFuture.get(), subJobFuture.get())
    }

    fun getImageByPath(path: String): ImageResource? {
        val segments = path.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            throw RuntimeException("path is empty : $path")
        }

        val drive = googleDriveHelper.drive
        val parentId = resolveFolderPath(segments.dropLast(1))
        val fileName = segments.last()
        val fileId = googleDriveCacheSaver.getFileId(path, drive, parentId, fileName)

        val metaFuture = asyncSupply(virtualThreadExecutor) {
            drive.files().get(fileId)
                .setFields("id, name, mimeType, size")
                .execute()
        }
        val downloadFuture = asyncSupply(virtualThreadExecutor) {
            val buffer = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(buffer)
            buffer.toByteArray()
        }

        val fileMeta = metaFuture.get()
        val mimeType = fileMeta.mimeType
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw RuntimeException("file is not image : ${fileMeta.name} (경로: $path)")
        }
        val bytes = downloadFuture.get()

        return ImageResource(
            inputStream = ByteArrayInputStream(bytes),
            mimeType = mimeType,
            fileName = fileMeta.name,
            fileSize = bytes.size.toLong()
        )
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
