package com.walter.lifelog.content.service

import com.walter.lifelog.content.entity.code.ContentType
import com.walter.lifelog.content.repository.ContentDocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContentService(
    private val contentDocumentRepository: ContentDocumentRepository,
) {
    @Transactional(readOnly = true)
    fun getContentByType(contentType: ContentType): Map<String, Any> {
        val contentDocument = contentDocumentRepository.findByContentType(contentType)
        return contentDocument?.content ?: emptyMap()
    }
}