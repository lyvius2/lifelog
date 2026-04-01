package com.walter.lifelog.worker.main.database.service

import com.walter.lifelog.shared.dto.ImageResource
import com.walter.lifelog.shared.dto.PhotoUpdateEventMessage
import com.walter.lifelog.shared.service.GoogleDriveService
import com.walter.lifelog.worker.main.database.repository.PhotosQueryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

@DisplayName("PhotoGenerateThumbnailService 테스트")
class PhotoGenerateThumbnailServiceTest {
    private val googleDriveService: GoogleDriveService = mockk()
    private val photosQueryRepository: PhotosQueryRepository = mockk(relaxed = true)

    private val service = PhotoGenerateThumbnailService(
        googleDriveService = googleDriveService,
        photosQueryRepository = photosQueryRepository,
    )

    private fun createMessage(
        photoSeq: Long = 1L,
        filePath: String = "lifelog/photos/sample.jpg",
    ) = PhotoUpdateEventMessage(
        photoSeq,
        filePath,
        "UPLOADED",
        LocalDateTime.now(),
        LocalDateTime.now(),
    )

    @Test
    @DisplayName("썸네일 생성 성공 시 true를 반환하고 상태가 READY로 업데이트된다")
    fun isSuccess_shouldReturnTrueAndUpdateStatusToReady() {
        // given
        val message = createMessage()
        val imageResource = ImageResource(
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            "image/jpeg",
            "parentId",
            "sample.jpg",
            3L,
        )
        every { googleDriveService.getImageByPath(message.filePath) } returns imageResource
        every { googleDriveService.generateThumbnail(imageResource) } returns mockk()

        // when
        val result = service.isSuccess(message)

        // then
        assertThat(result).isTrue()
        verifyOrder {
            photosQueryRepository.updatePhoto(1L, "PROCESSING", null)
            googleDriveService.getImageByPath("lifelog/photos/sample.jpg")
            googleDriveService.generateThumbnail(imageResource)
            photosQueryRepository.updatePhoto(1L, "READY", "lifelog/photos/thumb/sample_thumb.jpg")
        }
    }

    @Test
    @DisplayName("썸네일 생성 실패 시 false를 반환하고 상태가 FAILED로 업데이트된다")
    fun isSuccess_shouldReturnFalseAndUpdateStatusToFailed() {
        // given
        val message = createMessage()
        every { googleDriveService.getImageByPath(message.filePath) } throws RuntimeException("Drive error")

        // when
        val result = service.isSuccess(message)

        // then
        assertThat(result).isFalse()
        verify(exactly = 1) { photosQueryRepository.updatePhoto(1L, "PROCESSING", null) }
        verify(exactly = 1) { photosQueryRepository.updatePhoto(1L, "FAILED", null) }
        verify(exactly = 0) { photosQueryRepository.updatePhoto(any(), eq("READY"), any()) }
    }

    @Test
    @DisplayName("썸네일 경로가 올바르게 생성된다 - 중첩 폴더 경로")
    fun isSuccess_shouldGenerateCorrectThumbnailPathForNestedFolder() {
        // given
        val message = createMessage(photoSeq = 2L, filePath = "lifelog/archive/2026/photo.png")
        val imageResource = ImageResource(
            ByteArrayInputStream(byteArrayOf(1)),
            "image/png",
            "parentId",
            "photo.png",
            1L,
        )
        every { googleDriveService.getImageByPath(message.filePath) } returns imageResource
        every { googleDriveService.generateThumbnail(imageResource) } returns mockk()

        // when
        val result = service.isSuccess(message)

        // then
        assertThat(result).isTrue()
        verify(exactly = 1) {
            photosQueryRepository.updatePhoto(2L, "READY", "lifelog/archive/2026/thumb/photo_thumb.png")
        }
    }

    @Test
    @DisplayName("generateThumbnail 단계에서 예외 발생 시 FAILED 상태로 업데이트된다")
    fun isSuccess_shouldHandleExceptionDuringThumbnailGeneration() {
        // given
        val message = createMessage(photoSeq = 3L)
        val imageResource = ImageResource(
            ByteArrayInputStream(byteArrayOf(1)),
            "image/jpeg",
            "parentId",
            "sample.jpg",
            1L,
        )
        every { googleDriveService.getImageByPath(message.filePath) } returns imageResource
        every { googleDriveService.generateThumbnail(imageResource) } throws RuntimeException("Thumbnail generation failed")

        // when
        val result = service.isSuccess(message)

        // then
        assertThat(result).isFalse()
        verifyOrder {
            photosQueryRepository.updatePhoto(3L, "PROCESSING", null)
            photosQueryRepository.updatePhoto(3L, "FAILED", null)
        }
    }
}

