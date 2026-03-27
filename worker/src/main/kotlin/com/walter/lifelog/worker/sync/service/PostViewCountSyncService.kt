package com.walter.lifelog.worker.sync.service

import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import com.walter.lifelog.worker.sync.repository.PostsQueryRepository
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PostViewCountSyncService(
    private val postsQueryRepository: PostsQueryRepository,
    private val virtualThreadExecutor: TaskExecutor,
) {
    companion object {
        private const val BATCH_SIZE = 4
    }

    @Scheduled(cron = "0 0/10 * * * *")
    fun syncViewCounts() {
        val postSequences = postsQueryRepository.findPublishedPostSequences()
        postSequences.chunked(BATCH_SIZE).forEach { batch ->
            val futures = batch.map { postSeq ->
                asyncSupply(virtualThreadExecutor) { postsQueryRepository.updateViewCount(postSeq) }
            }
            futures.forEach { it.join() }
        }
    }
}