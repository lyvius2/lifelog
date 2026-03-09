package com.walter.lifelog.photo.service

import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.dto.UploadResponse
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoRepository
import com.walter.lifelog.shared.util.GoogleDriveHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Collections

@Service
class GoogleDriveService(
    private val googleDriveHelper: GoogleDriveHelper,
    private val photoRepository: PhotoRepository,
    private val photoMapper: PhotoMapper,
) {
    @Transactional
    fun uploadImage(uploadRequest: UploadRequest, folderPath: String, uploaderUserSeq: Long, file: MultipartFile): UploadResponse {
        require(!file.isEmpty) { "파일이 비어 있습니다." }
        val contentType = file.contentType
        require(contentType != null && contentType.startsWith("image/")) { "이미지 파일만 업로드 가능합니다: $contentType" }

        val drive = googleDriveHelper.drive
        val folders = folderPath.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var parentId = "root"
        for (folder in folders) {
            parentId = googleDriveHelper.findOrCreateFolder(drive, parentId, folder)
        }
        val fileMetadata = File().apply {
            name = file.originalFilename
            parents = Collections.singletonList(parentId)
        }

        val mediaContent = InputStreamContent(contentType, file.inputStream)
        val uploaded = drive.files().create(fileMetadata, mediaContent)
            .setFields("id, name, mimeType, size, webViewLink, webContentLink")
            .execute()
        photoRepository.save(photoMapper.toEntity(uploadRequest, uploaded, uploaderUserSeq, folderPath))
        return UploadResponse.of(uploaded, folderPath)
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
            inputStream = ByteArrayInputStream(bytes),
            mimeType = mimeType,
            fileName = fileMeta.name,
            fileSize = bytes.size.toLong()
        )
    }
}
