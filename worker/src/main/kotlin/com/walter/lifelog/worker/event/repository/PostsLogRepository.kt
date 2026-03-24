package com.walter.lifelog.worker.event.repository

import com.walter.lifelog.worker.event.entity.PostLog
import org.springframework.data.jpa.repository.JpaRepository

interface PostsLogRepository : JpaRepository<PostLog, Long> {
    fun findAllByPostSeqOrderByLogSeqDesc(postSeq: Long): List<PostLog>
}