package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PostsRepository : JpaRepository<Post, Long> {
    fun findBySlug(slug: String): Post?
    fun findByPostSeq(id: Long): Post?

    @Query("SELECT p FROM Post p WHERE p.categorySeq = :categorySeq AND p.status = 'PUBLISHED' AND p.createdAt < :createdAt ORDER BY p.createdAt DESC LIMIT 1")
    fun findPrevPost(categorySeq: Long, createdAt: LocalDateTime): Post?

    @Query("SELECT p FROM Post p WHERE p.categorySeq = :categorySeq AND p.status = 'PUBLISHED' AND p.createdAt > :createdAt ORDER BY p.createdAt ASC LIMIT 1")
    fun findNextPost(categorySeq: Long, createdAt: LocalDateTime): Post?
}