package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.entity.Post
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PostsRepository : JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = ["category"])
    fun findBySlug(slug: String): Post?

    @EntityGraph(attributePaths = ["category"])
    fun findByPostSeq(postSeq: Long): Post?

    @Query("SELECT p FROM Post p WHERE p.categorySeq = :categorySeq AND p.status = 'PUBLISHED' AND p.publishedAt < :publishedAt ORDER BY p.publishedAt DESC LIMIT 1")
    fun findPrevPost(categorySeq: Long, publishedAt: LocalDateTime?): Post?

    @Query("SELECT p FROM Post p WHERE p.categorySeq = :categorySeq AND p.status = 'PUBLISHED' AND p.publishedAt > :publishedAt ORDER BY p.publishedAt ASC LIMIT 1")
    fun findNextPost(categorySeq: Long, publishedAt: LocalDateTime?): Post?

    @Modifying
    @Query("UPDATE Post p SET p.status = 'ARCHIVED', p.updatedAt = :updatedAt WHERE p.postSeq = :postSeq")
    fun archivePost(postSeq: Long, updatedAt: LocalDateTime): Int
}