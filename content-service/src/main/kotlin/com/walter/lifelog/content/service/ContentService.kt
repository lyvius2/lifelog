package com.walter.lifelog.content.service

import com.walter.lifelog.content.dto.ContentDocumentRequest
import com.walter.lifelog.content.dto.ContentDocumentResponse
import com.walter.lifelog.content.dto.ContentTypeInfo
import com.walter.lifelog.content.entity.ContentDocuments
import com.walter.lifelog.content.entity.code.ContentType
import com.walter.lifelog.content.repository.ContentDocumentRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class ContentService(
    private val contentDocumentRepository: ContentDocumentRepository,
    private val cacheManager: CacheManager,
    private val dynamicCacheRegistry: DynamicCacheRegistry,
) {
    companion object {
        private const val CACHE_NAME = "contentByType"
        private const val CACHE_TTL_MINUTES = 5L
    }

    @DynamicCacheable(value = [CACHE_NAME], key = "#contentType.name", ttlMinutes = CACHE_TTL_MINUTES)
    fun getContentByType(contentType: ContentType): HashMap<String, Any> {
        val contentDocument = contentDocumentRepository.findByContentType(contentType)
        return contentDocument?.content ?: HashMap()
    }

    fun getAllTypeInfos(): List<ContentTypeInfo> {
        val existingTypes = contentDocumentRepository.findAll()
            .map { it.contentType }
            .toSet()
        return ContentType.entries.map { type ->
            ContentTypeInfo.of(type, existingTypes.contains(type))
        }
    }

    fun getDocumentByType(contentType: ContentType): ContentDocumentResponse {
        val document = contentDocumentRepository.findByContentType(contentType)
            ?: ContentDocuments(contentType = contentType, content = HashMap())
        return ContentDocumentResponse.of(document)
    }

    fun upsertDocument(request: ContentDocumentRequest): ContentDocumentResponse {
        val contentType = ContentType.valueOf(request.contentType)
        val existing = contentDocumentRepository.findByContentType(contentType)
        val saved = if (existing != null) {
            contentDocumentRepository.save(existing.copy(content = request.content))
        } else {
            contentDocumentRepository.save(
                ContentDocuments(contentType = contentType, content = request.content)
            )
        }
        evictContentCache(contentType)
        return ContentDocumentResponse.of(saved)
    }

    private fun evictContentCache(contentType: ContentType) {
        dynamicCacheRegistry.register(CACHE_NAME, CACHE_TTL_MINUTES)
        cacheManager.getCache(CACHE_NAME)?.evict(contentType.name)
    }
}
