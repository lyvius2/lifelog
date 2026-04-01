package com.walter.lifelog.worker.log.database.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.walter.lifelog.shared.dto.PostUpdateEventMessage
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "posts_log")
data class PostLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_seq", nullable = false)
    val logSeq: Long? = null,

    @Column(name = "post_seq", nullable = false)
    val postSeq: Long,

    @Column(name = "user_seq", nullable = false)
    val userSeq: Long,

    @Column(name = "category_seq")
    val categorySeq: Long? = null,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "slug", length = 200)
    val slug: String? = null,

    @Column(name = "summary", columnDefinition = "text")
    val summary: String? = null,

    @Column(name = "markdown_content", columnDefinition = "text")
    val markdownContent: String? = null,

    @Column(name = "status", length = 10)
    val status: String? = "DRAFT",

    @Column(name = "published_at")
    val publishedAt: LocalDateTime? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "log_created_at", updatable = false)
    var logCreatedAt: LocalDateTime? = null,
) {
    companion object {
        @JvmStatic
        fun of(message: PostUpdateEventMessage): PostLog {
            return PostLog(
                postSeq = message.postSeq,
                userSeq = message.userSeq,
                categorySeq = message.categorySeq,
                title = message.title,
                slug = message.slug,
                summary = message.summary,
                markdownContent = message.markdownContent,
                status = message.status,
                publishedAt = message.publishedAt
            )
        }
    }
}