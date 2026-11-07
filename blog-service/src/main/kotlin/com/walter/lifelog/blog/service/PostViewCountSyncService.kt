package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.repository.PostsQueryRepository
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import com.walter.lifelog.shared.util.ViewCountHelper
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Redis 에 누적된 게시글 조회수를 주기적으로 MySQL `posts.view_count` 에 반영합니다.
 * 조회 요청마다 RDB 를 쓰지 않기 위한 쓰기 지연 장치입니다.
 */
@Service
class PostViewCountSyncService(
    private val postsQueryRepository: PostsQueryRepository,
    private val viewCountHelper: ViewCountHelper,
    private val virtualThreadExecutor: TaskExecutor,
) {
    companion object {
        private const val BATCH_SIZE = 4
        private const val VIEW_COUNT_KEY_PREFIX = "post_"
    }

    private val log = LoggerFactory.getLogger(PostViewCountSyncService::class.java)

    @Scheduled(cron = "0 0/10 * * * *")
    fun syncViewCounts() {
        val postSequences = postsQueryRepository.findPublishedPostSequences()
        postSequences.chunked(BATCH_SIZE).forEach { batch ->
            val futures = batch.map { postSeq ->
                asyncSupply(virtualThreadExecutor) { syncViewCount(postSeq) }
            }
            futures.forEach { it.join() }
        }
    }

    private fun syncViewCount(postSeq: Long) {
        val viewCount = viewCountHelper.getCount("$VIEW_COUNT_KEY_PREFIX$postSeq") ?: return
        try {
            postsQueryRepository.updateViewCount(postSeq, viewCount)
        } catch (e: Exception) {
            log.error("Failed to sync view count — postSeq={}, viewCount={}", postSeq, viewCount, e)
        }
    }
}
