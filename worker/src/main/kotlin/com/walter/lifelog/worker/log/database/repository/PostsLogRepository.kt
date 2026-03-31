package com.walter.lifelog.worker.log.database.repository

import com.walter.lifelog.worker.log.database.entity.PostLog
import org.springframework.data.jpa.repository.JpaRepository

interface PostsLogRepository : JpaRepository<PostLog, Long> {
    fun findAllByPostSeqOrderByLogSeqDesc(postSeq: Long): List<PostLog>
}