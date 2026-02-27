package com.walter.lifelog.entity

import com.walter.lifelog.entity.code.ContentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "contents",
    indexes = [
        Index(name = "idx_content_type", columnList = "content_type")
    ]
)
data class Content(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_seq")
    val contentSeq: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    val contentType: ContentType,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "JSON", nullable = false)
    val content: Map<String, Any>,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null
)

