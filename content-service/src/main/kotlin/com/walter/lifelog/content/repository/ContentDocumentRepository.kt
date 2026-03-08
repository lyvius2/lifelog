package com.walter.lifelog.content.repository

import com.walter.lifelog.content.entity.ContentDocuments
import com.walter.lifelog.content.entity.code.ContentType
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentDocumentRepository : MongoRepository<ContentDocuments, String> {
    fun findByContentType(contentType: ContentType): ContentDocuments?
}