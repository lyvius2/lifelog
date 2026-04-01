package com.walter.lifelog.worker.log.database.service

import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.worker.log.database.entity.PhotoLog
import com.walter.lifelog.worker.log.database.repository.PhotosLogRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class PhotoLogUpdateService(
    private val photosLogRepository: PhotosLogRepository,
) {
    @Transactional
    fun saveFailLog(message: PhotoUpdateEventMessage) {
        photosLogRepository.save(PhotoLog.of(message, "FAILED"))
    }
}