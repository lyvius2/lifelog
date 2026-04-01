package com.walter.lifelog.worker.log.database.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
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
@Table(name = "photos_log")
data class PhotoLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_seq", nullable = false)
    val logSeq: Long? = null,

    @Column(name = "photo_seq", nullable = false)
    val photoSeq: Long,

    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,

    @Column(name = "status", nullable = false, length = 10)
    val status: String?,

    @Column(name = "is_completed", nullable = false)
    val isCompleted: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "log_created_at", updatable = false)
    var logCreatedAt: LocalDateTime? = null,
) {
    companion object {
        @JvmStatic
        fun of(message: PhotoUpdateEventMessage, status: String): PhotoLog {
            return PhotoLog(
                photoSeq = message.photoSeq,
                imageUrl = message.filePath,
                status = status,
                createdAt = message.createdAt,
                updatedAt = message.updatedAt,
            )
        }
    }
}
