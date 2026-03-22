package com.walter.lifelog.blog.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Aspect
@Component
class PostSaveEventAspect(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val postsRepository: PostsRepository,
    private val virtualThreadExecutor: TaskExecutor,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(PostSaveEventAspect::class.java)

    @AfterReturning(
        pointcut = "execution(* com.walter.lifelog.blog.facade.PostFacade.savePost(..))",
        returning = "result"
    )
    fun afterSavePost(result: PostSaveResponse) {
        asyncSupply(virtualThreadExecutor) { publishPostSavedEvent(result.postSeq) }
    }

    fun publishPostSavedEvent(postSeq: Long) {
        try {
            val message = objectMapper.writeValueAsString(postsRepository.findByPostSeq(postSeq))
            kafkaTemplate.send(KafkaTopics.POST_UPDATED, message)
        } catch (e: Exception) {
            log.error("Failed to publish post saved event — postSeq={}", Long, e)
        }
    }
}

