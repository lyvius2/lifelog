package com.walter.lifelog.content.repository

import com.walter.lifelog.content.entity.Content
import com.walter.lifelog.content.entity.code.ContentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentsRepository : JpaRepository<Content, Long> {
    fun findByContentType(contentType: ContentType): Content?
}