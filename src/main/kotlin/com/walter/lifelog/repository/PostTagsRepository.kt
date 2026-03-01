package com.walter.lifelog.repository

import com.walter.lifelog.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostTagsRepository : JpaRepository<PostTag, Long> {
    fun findByPostSeq(postSeq: Long): List<PostTag>
    fun deleteByPostSeq(postSeq: Long)
}