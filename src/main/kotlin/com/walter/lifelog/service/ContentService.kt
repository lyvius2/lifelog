package com.walter.lifelog.service

import com.walter.lifelog.entity.code.ContentType
import com.walter.lifelog.repository.ContentsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContentService(
    private val contentsRepository: ContentsRepository
) {
    @Transactional(readOnly = true)
    fun getContentByType(contentType: ContentType): Map<String, Any>? {
        val content = contentsRepository.findByContentType(contentType) ?: return null
        return content.content
    }
}

