package com.walter.lifelog.content.service

import com.walter.lifelog.content.entity.code.ContentType
import com.walter.lifelog.content.repository.ContentDocumentRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContentService(
    private val contentDocumentRepository: ContentDocumentRepository,
) {
    @DynamicCacheable(value = ["contentByType"], key = "#contentType.name", ttlMinutes = 5)
    @Transactional(readOnly = true)
    fun getContentByType(contentType: ContentType): HashMap<String, Any> {
        val contentDocument = contentDocumentRepository.findByContentType(contentType)
        return contentDocument?.content ?: HashMap()
    }
}