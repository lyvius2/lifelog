package com.walter.lifelog.worker.log.database.service

import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.worker.log.database.entity.PhotoLog
import com.walter.lifelog.worker.log.database.repository.PhotosLogRepository
import io.mockk.mockk
import io.mockk.slot
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("PhotoLogUpdateService 테스트")
class PhotoLogUpdateServiceTest {

    private val photosLogRepository: PhotosLogRepository = mockk()

    private val service = PhotoLogUpdateService(
        photosLogRepository = photosLogRepository,
    )

    @Test
    @DisplayName("saveFailLog - 실패 메시지를 받아 FAILED 상태의 PhotoLog를 저장한다")
    fun saveFailLog_shouldSavePhotoLogWithFailedStatus() {
        // given
        val now = LocalDateTime.of(2026, 4, 1, 12, 0, 0)
        val message = PhotoUpdateEventMessage(
            10L,
            "lifelog/photos/sample.jpg",
            "UPLOADED",
            now,
            now,
        )
        val logSlot = slot<PhotoLog>()
        every { photosLogRepository.save(capture(logSlot)) } answers { logSlot.captured }

        // when
        service.saveFailLog(message)

        // then
        verify(exactly = 1) { photosLogRepository.save(any()) }
        val savedLog = logSlot.captured
        assertThat(savedLog.photoSeq).isEqualTo(10L)
        assertThat(savedLog.imageUrl).isEqualTo("lifelog/photos/sample.jpg")
        assertThat(savedLog.status).isEqualTo("FAILED")
        assertThat(savedLog.isCompleted).isFalse()
        assertThat(savedLog.createdAt).isEqualTo(now)
        assertThat(savedLog.updatedAt).isEqualTo(now)
        assertThat(savedLog.logSeq).isNull()
        assertThat(savedLog.completedAt).isNull()
    }
}

