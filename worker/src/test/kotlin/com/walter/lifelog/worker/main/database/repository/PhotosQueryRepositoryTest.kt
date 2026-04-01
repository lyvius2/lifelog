package com.walter.lifelog.worker.main.database.repository

import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PhotosQueryRepository 테스트")
class PhotosQueryRepositoryTest {

    private val dsl: DSLContext = mockk(relaxed = true)
    private val repository = PhotosQueryRepository(dsl)

    @Test
    @DisplayName("updatePhoto - status와 thumbnailUrl을 함께 업데이트한다")
    fun updatePhoto_shouldUpdateStatusAndThumbnailUrl() {
        // given
        val photoSeq = 1L
        val status = "READY"
        val thumbnailUrl = "lifelog/photos/thumb/sample_thumb.jpg"

        // when
        repository.updatePhoto(photoSeq, status, thumbnailUrl)

        // then
        verify { dsl.update(any<Table<*>>()) }
    }

    @Test
    @DisplayName("updatePhoto - thumbnailUrl이 null이면 status만 업데이트한다")
    fun updatePhoto_shouldUpdateOnlyStatusWhenThumbnailUrlIsNull() {
        // given
        val photoSeq = 2L
        val status = "PROCESSING"

        // when
        repository.updatePhoto(photoSeq, status, null)

        // then
        verify { dsl.update(any<Table<*>>()) }
    }

    @Test
    @DisplayName("updatePhoto - FAILED 상태로 업데이트한다")
    fun updatePhoto_shouldUpdateToFailedStatus() {
        // given
        val photoSeq = 3L
        val status = "FAILED"

        // when
        repository.updatePhoto(photoSeq, status, null)

        // then
        verify { dsl.update(any<Table<*>>()) }
    }
}

