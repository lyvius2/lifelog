package com.walter.lifelog.worker.consume

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.worker.log.database.service.PhotoLogUpdateService
import com.walter.lifelog.worker.main.database.service.PhotoGenerateThumbnailService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PhotoUpdatedEventConsumer(
    private val objectMapper: ObjectMapper,
    private val photoGenerateThumbnailService: PhotoGenerateThumbnailService,
    private val photoLogUpdateService: PhotoLogUpdateService,
) {
    @KafkaListener(topics = [KafkaTopics.PHOTO_UPDATED])
    fun consume(message: String) {
        val photoEvent = objectMapper.readValue(message, PhotoUpdateEventMessage::class.java)
        val isGenerateSuccess = photoGenerateThumbnailService.isSuccess(photoEvent)
        if (!isGenerateSuccess) {
            photoLogUpdateService.saveFailLog(photoEvent)
        }
    }
}