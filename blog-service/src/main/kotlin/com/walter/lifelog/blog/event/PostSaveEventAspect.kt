package com.walter.lifelog.blog.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.blog.mapper.PostMapper
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Aspect
@Component
class PostSaveEventAspect(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val postsRepository: PostsRepository,
    private val objectMapper: ObjectMapper,
    private val postMapper: PostMapper,
) {
    private val log = LoggerFactory.getLogger(PostSaveEventAspect::class.java)

    @AfterReturning(pointcut = "execution(* com.walter.lifelog.blog.facade.PostFacade.savePost(..))", returning = "result")
    fun publishPostUpdatedEvent(postSeq: Long) {
        try {
            val post = postsRepository.findByPostSeq(postSeq) ?: throw IllegalArgumentException("Post not found with postSeq.")
            val message = objectMapper.writeValueAsString(postMapper.toEventMessage(post))
            kafkaTemplate.send(KafkaTopics.POST_UPDATED, message)
        } catch (e: Exception) {
            log.error("Failed to publish post saved event — postSeq={}", Long, e)
        }
    }
}

