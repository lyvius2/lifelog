package com.walter.lifelog.photo.service

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.ImageResource
import com.walter.lifelog.shared.util.GoogleDriveHelper
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

@Service
class GoogleDriveService(
    private val virtualThreadExecutor: TaskExecutor,
    private val googleDriveHelper: GoogleDriveHelper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 경로 → 폴더 ID 캐시 */
    private val folderIdCache = ConcurrentHashMap<String, String>()

    /** 파일 경로 → 파일 ID 캐시 */
    private val fileIdCache = ConcurrentHashMap<String, String>()

    @Transactional
    fun uploadImage(folderPath: String, file: MultipartFile): Pair<File, File> {
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
        val mainJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.uploadFile(file.originalFilename, parentId, file.inputStream, contentType)
        }
        val subJobFuture = asyncSupply(virtualThreadExecutor) {
            googleDriveHelper.generateThumbnail(file.originalFilename, parentId, file.inputStream, contentType)
        }

        // 업로드 후 해당 폴더 경로의 파일 ID 캐시 무효화
        fileIdCache.keys.removeIf { it.startsWith(folderPath) }

        return Pair(mainJobFuture.get(), subJobFuture.get())
    }

    fun getImageByPath(path: String): ImageResource? {
        val segments = path.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            throw RuntimeException("경로가 비어 있습니다: $path")
        }

        val drive = googleDriveHelper.drive

        // 폴더 ID 캐시를 활용한 폴더 경로 탐색
        val parentId = resolveFolderPath(segments.dropLast(1))

        // 파일 ID 캐시 활용
        val fileName = segments.last()
        val fileId = fileIdCache.getOrPut(path) {
            log.debug("File ID cache miss: {}", path)
            googleDriveHelper.findFileId(drive, parentId, fileName, false)
                ?: throw RuntimeException("파일을 찾을 수 없습니다: $fileName (경로: $path)")
        }

        // Virtual Thread로 메타데이터 조회와 파일 다운로드를 병렬 실행
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
            throw RuntimeException("파일이 이미지가 아닙니다: ${fileMeta.getName()} (경로: $path)")
        }

        val bytes = downloadFuture.get()

        return ImageResource(
            inputStream = ByteArrayInputStream(bytes),
            mimeType = mimeType,
            fileName = fileMeta.name,
            fileSize = bytes.size.toLong()
        )
    }

    /**
     * 폴더 경로를 순회하며 폴더 ID를 찾고, 캐시에 저장한다.
     */
    private fun resolveFolderPath(folders: List<String>): String {
        if (folders.isEmpty()) return "root"

        val drive = googleDriveHelper.drive
        var parentId = "root"
        val pathBuilder = StringBuilder()

        for (folder in folders) {
            if (pathBuilder.isNotEmpty()) pathBuilder.append("/")
            pathBuilder.append(folder)
            val cacheKey = pathBuilder.toString()

            parentId = folderIdCache.getOrPut(cacheKey) {
                googleDriveHelper.findFileId(drive, parentId, folder, true)
                    ?: throw RuntimeException("폴더를 찾을 수 없습니다: $folder")
            }
        }
        return parentId
    }
}
