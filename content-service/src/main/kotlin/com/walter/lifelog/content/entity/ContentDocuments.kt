package com.walter.lifelog.content.entity

import com.walter.lifelog.content.entity.code.ContentType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 타임스탬프는 [com.walter.lifelog.content.service.ContentService] 가 직접 넣습니다.
 * 이 프로젝트는 `@EnableMongoAuditing` 을 켜지 않아 `@CreatedDate` 류가 동작하지 않습니다.
 */
@Document(collection = "content-documents")
data class ContentDocuments(
    @Id
    val id: String? = null,

    @Indexed
    val contentType: ContentType,

    val content: HashMap<String, Any>,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime? = null
)
