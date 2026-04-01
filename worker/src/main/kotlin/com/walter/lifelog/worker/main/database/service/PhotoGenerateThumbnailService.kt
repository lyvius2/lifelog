package com.walter.lifelog.worker.main.database.service

import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.shared.service.GoogleDriveService
import com.walter.lifelog.worker.main.database.repository.PhotosQueryRepository
import org.springframework.stereotype.Service

@Service
class PhotoGenerateThumbnailService(
    private val googleDriveService: GoogleDriveService,
    private val photosQueryRepository: PhotosQueryRepository,
) {
    fun isSuccess(message: PhotoUpdateEventMessage): Boolean {
        try {
            photosQueryRepository.updatePhoto(message.photoSeq, "PROCESSING", null)
            val imageResource = googleDriveService.getImageByPath(message.filePath)
            googleDriveService.generateThumbnail(imageResource)
            photosQueryRepository.updatePhoto(message.photoSeq, "READY", getThumbnailPath(message))
            return true
        } catch (_: Exception) {
            photosQueryRepository.updatePhoto(message.photoSeq, "FAILED", null)
            return false
        }
    }

    private fun getThumbnailPath(message: PhotoUpdateEventMessage): String {
        return message.filePath
            .substringBeforeLast("/")
            .let { "$it/thumb" } + "/" + message.filePath
                .substringAfterLast("/")
                .substringBeforeLast(".")
                .let { "${it}_thumb" } + "." + message.filePath.substringAfterLast(".")
    }
}