package com.walter.lifelog.content.entity

import com.walter.lifelog.content.entity.code.ContentType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "content-documents")
data class ContentDocuments(
    @Id
    val id: String? = null,

    @Indexed
    val contentType: ContentType,

    val content: Map<String, Any>,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    val updatedAt: LocalDateTime? = null
)
