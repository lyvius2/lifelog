package com.walter.lifelog.worker.consume

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.worker.log.database.entity.PostLog
import com.walter.lifelog.worker.log.database.repository.PostsLogRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PostsLogConsumer(
    private val postsLogRepository: PostsLogRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    @KafkaListener(topics = [KafkaTopics.POST_UPDATED])
    fun consume(message: String) {
        val postLog = objectMapper.readValue(message, PostLog::class.java)
        postsLogRepository.save(postLog)
    }
}