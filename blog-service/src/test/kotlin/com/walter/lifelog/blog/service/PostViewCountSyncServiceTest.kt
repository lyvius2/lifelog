package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.repository.PostsQueryRepository
import com.walter.lifelog.shared.util.ViewCountHelper
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
    private val viewCountHelper: ViewCountHelper = mockk()
    private val virtualThreadExecutor: TaskExecutor = TaskExecutor { task ->
        Executors.newVirtualThreadPerTaskExecutor().execute(task)
    }

    private val sut = PostViewCountSyncService(
        postsQueryRepository = postsQueryRepository,
        viewCountHelper = viewCountHelper,
        virtualThreadExecutor = virtualThreadExecutor,
    )

    @Test
    @DisplayName("syncViewCounts - 발행된 게시글들의 조회수를 Redis에서 읽어 DB에 동기화한다")
    fun syncViewCounts_publishedPosts_updatesEachViewCount() {
        // given
        val postSequences = listOf(1L, 2L, 3L, 5L, 8L, 13L)
        every { postsQueryRepository.findPublishedPostSequences() } returns postSequences
        every { viewCountHelper.getCount(any()) } returns 150
        every { postsQueryRepository.updateViewCount(any(), any()) } returns 1

        // when
        sut.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.findPublishedPostSequences() }
        postSequences.forEach { postSeq ->
            verify(exactly = 1) { viewCountHelper.getCount("post_$postSeq") }
            verify(exactly = 1) { postsQueryRepository.updateViewCount(postSeq, 150) }
        }
    }

    @Test
    @DisplayName("syncViewCounts - 발행된 게시글이 없으면 updateViewCount를 호출하지 않는다")
    fun syncViewCounts_noPublishedPosts_doesNotUpdate() {
        // given
        every { postsQueryRepository.findPublishedPostSequences() } returns emptyList()

        // when
        sut.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.findPublishedPostSequences() }
        verify(exactly = 0) { postsQueryRepository.updateViewCount(any(), any()) }
    }

    @Test
    @DisplayName("syncViewCounts - BATCH_SIZE(4)를 넘는 목록도 청크 단위로 전부 처리한다")
    fun syncViewCounts_moreThanBatchSize_processesEveryPost() {
        // given
        val postSequences = listOf(1L, 2L, 3L, 4L, 5L)
        every { postsQueryRepository.findPublishedPostSequences() } returns postSequences
        every { viewCountHelper.getCount(any()) } returns 7
        every { postsQueryRepository.updateViewCount(any(), any()) } returns 1

        // when
        sut.syncViewCounts()

        // then
        verify(exactly = 5) { postsQueryRepository.updateViewCount(any(), any()) }
    }

    @Test
    @DisplayName("syncViewCounts - Redis에 조회수가 없는 게시글은 DB를 갱신하지 않는다")
    fun syncViewCounts_missingRedisValue_skipsUpdate() {
        // given
        every { postsQueryRepository.findPublishedPostSequences() } returns listOf(1L, 2L)
        every { viewCountHelper.getCount("post_1") } returns 42
        every { viewCountHelper.getCount("post_2") } returns null
        every { postsQueryRepository.updateViewCount(1L, 42) } returns 1

        // when
        sut.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.updateViewCount(1L, 42) }
        verify(exactly = 0) { postsQueryRepository.updateViewCount(2L, any()) }
    }

    @Test
    @DisplayName("syncViewCounts - 특정 게시글 갱신이 실패해도 나머지 게시글 동기화는 계속한다")
    fun syncViewCounts_updateFailure_continuesRemainingPosts() {
        // given
        every { postsQueryRepository.findPublishedPostSequences() } returns listOf(1L, 2L)
        every { viewCountHelper.getCount(any()) } returns 10
        every { postsQueryRepository.updateViewCount(1L, 10) } throws IllegalStateException("db down")
        every { postsQueryRepository.updateViewCount(2L, 10) } returns 1

        // when
        sut.syncViewCounts()

        // then
        verify(exactly = 1) { postsQueryRepository.updateViewCount(2L, 10) }
    }
}
