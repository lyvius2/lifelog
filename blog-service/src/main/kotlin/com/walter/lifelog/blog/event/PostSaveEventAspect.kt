package com.walter.lifelog.blog.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.shared.config.messaging.KafkaTopics
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

    @AfterReturning(
        pointcut = "execution(* com.walter.lifelog.blog.facade.PostFacade.savePost(..))",
        returning = "result"
    )
    fun afterSavePost(joinPoint: JoinPoint, result: PostSaveResponse) {
        val postRequest = joinPoint.args[0] as PostRequest
        val userSeq = joinPoint.args[1] as Long
        asyncSupply(virtualThreadExecutor) { publishPostSavedEvent(result, postRequest, userSeq) }
    }

    fun publishPostSavedEvent(result: PostSaveResponse, postRequest: PostRequest, userSeq: Long) {
        try {
            val message = PostSavedMessage(
                postSeq = result.postSeq,
                userSeq = userSeq,
                categorySeq = postRequest.categorySeq,
                title = postRequest.title,
                slug = postRequest.slug,
                summary = postRequest.summary,
                markdownContent = postRequest.markdownContent,
                status = postRequest.status,
                publishedAt = postRequest.publishedAt,
            )
            kafkaTemplate.send(KafkaTopics.POST_UPDATED, objectMapper.writeValueAsString(message))
        } catch (e: Exception) {
            log.error("Failed to publish post saved event — postSeq={}", result.postSeq, e)
        }
    }

    data class PostSavedMessage(
        val postSeq: Long,
        val userSeq: Long,
        val categorySeq: Long,
        val title: String,
        val slug: String?,
        val summary: String?,
        val markdownContent: String?,
        val status: String,
        val publishedAt: String?,
    )
}

