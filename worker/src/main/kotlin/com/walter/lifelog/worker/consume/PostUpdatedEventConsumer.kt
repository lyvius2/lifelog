package com.walter.lifelog.worker.consume

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.shared.dto.PostUpdateEventMessage
import com.walter.lifelog.worker.log.database.service.PostLogUpdateService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PostUpdatedEventConsumer(
    private val postLogUpdateService: PostLogUpdateService,
    private val objectMapper: ObjectMapper
) {
    @KafkaListener(topics = [KafkaTopics.POST_UPDATED])
    fun consume(message: String) {
        val postEvent = objectMapper.readValue(message, PostUpdateEventMessage::class.java)
        postLogUpdateService.saveLog(postEvent)
    }
}