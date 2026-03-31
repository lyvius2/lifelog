package com.walter.lifelog.worker.consume

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.shared.service.GoogleDriveService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PhotoUploadEventConsumer(
    private val objectMapper: ObjectMapper,
    private val googleDriveService: GoogleDriveService,
) {
    @KafkaListener(topics = [KafkaTopics.PHOTO_UPDATED])
    fun consume(message: String) {
        val photoEvent = objectMapper.readValue(message, PhotoUpdateEventMessage::class.java)
        // TODO: 상태 업데이트 ('UPLOADED' -> 'PROCESSING')
        val imageResource = googleDriveService.getImageByPath(photoEvent.filePath)
        googleDriveService.generateThumbnail(imageResource)
        // TODO: 상태 업데이트 ('PROCESSING' -> 'READY' or 'FAILED')
    }
}