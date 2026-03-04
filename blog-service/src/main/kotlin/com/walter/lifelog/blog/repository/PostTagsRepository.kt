package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostTagsRepository : JpaRepository<PostTag, Long> {
    fun findByPostSeq(postSeq: Long): List<PostTag>
    fun deleteByPostSeq(postSeq: Long)
}