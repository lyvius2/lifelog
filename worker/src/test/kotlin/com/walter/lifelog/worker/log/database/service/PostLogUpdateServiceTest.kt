package com.walter.lifelog.worker.log.database.service

import com.walter.lifelog.shared.dto.PostUpdateEventMessage
import com.walter.lifelog.worker.log.database.entity.PostLog
import com.walter.lifelog.worker.log.database.repository.PostsLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("PostLogUpdateService 테스트")
class PostLogUpdateServiceTest {

    private val postsLogRepository: PostsLogRepository = mockk()

    private val service = PostLogUpdateService(
        postsLogRepository = postsLogRepository,
    )

    @Test
    @DisplayName("saveLog - PostUpdateEventMessage를 받아 PostLog로 변환하여 저장한다")
    fun saveLog_shouldSavePostLogFromMessage() {
        // given
        val now = LocalDateTime.of(2026, 4, 1, 12, 0, 0)
        val message = PostUpdateEventMessage(
            5L,
            1L,
            3L,
            "테스트 게시글",
            "test-post",
            "요약입니다",
            "# 마크다운 본문",
            "PUBLISHED",
            now,
            now.minusDays(1),
            now,
        )
        val logSlot = slot<PostLog>()
        every { postsLogRepository.save(capture(logSlot)) } answers { logSlot.captured }

        // when
        service.saveLog(message)

        // then
        verify(exactly = 1) { postsLogRepository.save(any()) }
        val savedLog = logSlot.captured
        assertThat(savedLog.logSeq).isNull()
        assertThat(savedLog.postSeq).isEqualTo(5L)
        assertThat(savedLog.userSeq).isEqualTo(1L)
        assertThat(savedLog.categorySeq).isEqualTo(3L)
        assertThat(savedLog.title).isEqualTo("테스트 게시글")
        assertThat(savedLog.slug).isEqualTo("test-post")
        assertThat(savedLog.summary).isEqualTo("요약입니다")
        assertThat(savedLog.markdownContent).isEqualTo("# 마크다운 본문")
        assertThat(savedLog.status).isEqualTo("PUBLISHED")
        assertThat(savedLog.publishedAt).isEqualTo(now)
        assertThat(savedLog.createdAt).isEqualTo(now.minusDays(1))
        assertThat(savedLog.updatedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("saveLog - nullable 필드가 null인 메시지도 정상적으로 저장한다")
    fun saveLog_shouldHandleNullableFields() {
        // given
        val now = LocalDateTime.of(2026, 4, 1, 10, 0, 0)
        val message = PostUpdateEventMessage(
            7L,
            2L,
            null,
            "임시 저장 게시글",
            null,
            null,
            null,
            "DRAFT",
            null,
            now,
            now,
        )
        val logSlot = slot<PostLog>()
        every { postsLogRepository.save(capture(logSlot)) } answers { logSlot.captured }

        // when
        service.saveLog(message)

        // then
        verify(exactly = 1) { postsLogRepository.save(any()) }
        val savedLog = logSlot.captured
        assertThat(savedLog.postSeq).isEqualTo(7L)
        assertThat(savedLog.userSeq).isEqualTo(2L)
        assertThat(savedLog.categorySeq).isNull()
        assertThat(savedLog.title).isEqualTo("임시 저장 게시글")
        assertThat(savedLog.slug).isNull()
        assertThat(savedLog.summary).isNull()
        assertThat(savedLog.markdownContent).isNull()
        assertThat(savedLog.status).isEqualTo("DRAFT")
        assertThat(savedLog.publishedAt).isNull()
    }
}

