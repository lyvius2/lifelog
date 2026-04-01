package com.walter.lifelog.worker.log.database.service

import com.walter.lifelog.shared.dto.PostUpdateEventMessage
import com.walter.lifelog.worker.log.database.entity.PostLog
import com.walter.lifelog.worker.log.database.repository.PostsLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLogUpdateService(
    val postsLogRepository: PostsLogRepository,
) {
    @Transactional
    fun saveLog(message: PostUpdateEventMessage) {
        postsLogRepository.save(PostLog.of(message))
    }
}