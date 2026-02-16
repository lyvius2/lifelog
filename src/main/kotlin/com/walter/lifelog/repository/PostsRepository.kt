package com.walter.lifelog.repository

import com.walter.lifelog.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostsRepository : JpaRepository<Post, Long> {
    fun findBySlug(slug: String): Post?
    fun findByPostSeq(id: Long): Post?
}