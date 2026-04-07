package com.walter.lifelog.blog.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.shared.config.messaging.KafkaTopics
import com.walter.lifelog.shared.config.messaging.PostUpdatedMessage
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.aspectj.lang.JoinPoint
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
    private val virtualThreadExecutor: TaskExecutor,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(PostSaveEventAspect::class.java)

    @AfterReturning(pointcut = "execution(* com.walter.lifelog.blog.facade.PostFacade.savePost(..))", returning = "result")
    fun afterSavePost(joinPoint: JoinPoint, result: PostSaveResponse) {
        val postRequest = joinPoint.args[0] as PostRequest
        val userSeq = joinPoint.args[1] as Long
        asyncSupply(virtualThreadExecutor) { publishPostSavedEvent(result, postRequest, userSeq) }
    }

    fun publishPostSavedEvent(result: PostSaveResponse, postRequest: PostRequest, userSeq: Long) {
        try {
            val message = PostUpdatedMessage(
                result.postSeq, userSeq, postRequest.categorySeq, result.title, postRequest.slug, postRequest.summary,
                postRequest.markdownContent, postRequest.status, result.publishedAt, result.createdAt, result.updatedAt
            )
            kafkaTemplate.send(KafkaTopics.POST_UPDATED, objectMapper.writeValueAsString(message))
        } catch (e: Exception) {
            log.error("Failed to publish post saved event — postSeq={}", result.postSeq, e)
        }
    }
}

