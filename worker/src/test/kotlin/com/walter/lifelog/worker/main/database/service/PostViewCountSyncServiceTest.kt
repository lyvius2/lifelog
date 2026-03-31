package com.walter.lifelog.worker.main.database.service

import com.walter.lifelog.worker.main.database.repository.PostsQueryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import java.util.concurrent.Executors

@DisplayName("PostViewCountSyncService 테스트")
class PostViewCountSyncServiceTest {
    private val postsQueryRepository: PostsQueryRepository = mockk()
    private val virtualThreadExecutor: TaskExecutor = TaskExecutor { task ->
        Executors.newVirtualThreadPerTaskExecutor().execute(task)
    }

    private val service = PostViewCountSyncService(
        postsQueryRepository = postsQueryRepository,
        virtualThreadExecutor = virtualThreadExecutor,
    )

    @Test
    @DisplayName("syncViewCounts - 발행된 게시글들의 조회수를 Redis에서 읽어 DB에 동기화한다")
    fun syncViewCounts_shouldUpdateViewCountsForPublishedPosts() {
        // given
        val postSequences = listOf(1L, 2L, 3L, 5L, 8L, 13L)
        every { postsQueryRepository.findPublishedPostSequences() } returns postSequences
        every { postsQueryRepository.updateViewCount(any()) } returns 1

        // when
        service.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.findPublishedPostSequences() }
        postSequences.forEach { postSeq ->
            verify(exactly = 1) { postsQueryRepository.updateViewCount(postSeq) }
        }
    }

    @Test
    @DisplayName("syncViewCounts - 발행된 게시글이 없으면 updateViewCount를 호출하지 않는다")
    fun syncViewCounts_shouldNotUpdateWhenNoPublishedPosts() {
        // given
        every { postsQueryRepository.findPublishedPostSequences() } returns emptyList()

        // when
        service.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.findPublishedPostSequences() }
        verify(exactly = 0) { postsQueryRepository.updateViewCount(any()) }
    }

    @Test
    @DisplayName("syncViewCounts - BATCH_SIZE(4)에 따라 청크 단위로 처리한다")
    fun syncViewCounts_shouldProcessInBatches() {
        // given
        val postSequences = listOf(1L, 2L, 3L, 4L, 5L)
        every { postsQueryRepository.findPublishedPostSequences() } returns postSequences
        every { postsQueryRepository.updateViewCount(any()) } returns 1

        // when
        service.syncViewCounts()

        // then
        verify(exactly = 5) { postsQueryRepository.updateViewCount(any()) }
    }

    @Test
    @DisplayName("syncViewCounts - Redis에 조회수가 없는 게시글은 0을 반환한다")
    fun syncViewCounts_shouldHandleZeroViewCount() {
        // given
        val postSequences = listOf(1L, 2L)
        every { postsQueryRepository.findPublishedPostSequences() } returns postSequences
        every { postsQueryRepository.updateViewCount(1L) } returns 1
        every { postsQueryRepository.updateViewCount(2L) } returns 0

        // when
        service.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.updateViewCount(1L) }
        verify(exactly = 1) { postsQueryRepository.updateViewCount(2L) }
    }
}

