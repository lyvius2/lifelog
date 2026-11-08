package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoShotPeriod
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoriesRepository
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.photo.repository.PhotosQueryRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PhotoService 테스트")
class PhotoServiceTest {

    private val photoMapper: PhotoMapper = mockk()
    private val photosRepository: PhotosRepository = mockk()
    private val photoCategoriesRepository: PhotoCategoriesRepository = mockk()
    private val photoTagsRepository: PhotoTagsRepository = mockk()
    private val photosQueryRepository: PhotosQueryRepository = mockk()

    private val sut = PhotoService(
        photoMapper = photoMapper,
        photosRepository = photosRepository,
        photoCategoriesRepository = photoCategoriesRepository,
        photoTagsRepository = photoTagsRepository,
        photosQueryRepository = photosQueryRepository,
    )

    @Test
    @DisplayName("getActivePhotoCategories - 리포지토리가 투영한 응답을 그대로 전달한다")
    fun getActivePhotoCategories_hasCategories_returnsAsIs() {
        // given
        val responses = listOf(
            PhotoCategoryResponse(categorySeq = 1L, icon = "🚗", name = "My Car"),
            PhotoCategoryResponse(categorySeq = 2L, icon = "🌿", name = "Nature"),
        )
        every { photoCategoriesRepository.findActiveCategoryResponses() } returns responses

        // when
        val result = sut.getActivePhotoCategories()

        // then
        assertThat(result).isEqualTo(responses)
        verify(exactly = 1) { photoCategoriesRepository.findActiveCategoryResponses() }
    }

    @Test
    @DisplayName("getActivePhotoCategories - 활성 카테고리가 없으면 빈 목록을 돌려준다")
    fun getActivePhotoCategories_noCategories_returnsEmpty() {
        // given
        every { photoCategoriesRepository.findActiveCategoryResponses() } returns emptyList()

        // when
        val result = sut.getActivePhotoCategories()

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { photoCategoriesRepository.findActiveCategoryResponses() }
    }

    @Test
    @DisplayName("getPhotoShotPeriod - 집계 결과를 그대로 돌려준다")
    fun getPhotoShotPeriod_hasPhotos_returnsAggregated() {
        // given
        val period = PhotoShotPeriod(minYear = 2015, maxYear = 2026)
        every { photosRepository.findShotPeriod() } returns period

        // when
        val result = sut.getPhotoShotPeriod()

        // then
        assertThat(result).isEqualTo(period)
        verify(exactly = 1) { photosRepository.findShotPeriod() }
    }

    @Test
    @DisplayName("updateLikeCount - 좋아요 수만 갱신하는 쿼리를 한 번 호출한다")
    fun updateLikeCount_anyPhoto_updatesOnlyLikeCount() {
        // given
        val photoSeq = 1L
        every { photosRepository.updateLikeCount(photoSeq, 10) } returns 1

        // when
        sut.updateLikeCount(photoSeq, 10)

        // then
        verify(exactly = 1) { photosRepository.updateLikeCount(photoSeq, 10) }
    }

    @Test
    @DisplayName("deletePhoto - is_active 를 false 로 바꾸는 논리 삭제를 수행한다")
    fun deletePhoto_anyPhoto_marksInactive() {
        // given
        val photoSeq = 1L
        every { photosRepository.updateActive(photoSeq, false) } returns 1

        // when
        sut.deletePhoto(photoSeq)

        // then
        verify(exactly = 1) { photosRepository.updateActive(photoSeq, false) }
    }

    @Test
    @DisplayName("deletePhoto - 없는 사진이면 갱신 0건으로 끝난다 (조회 왕복이 없다)")
    fun deletePhoto_missingPhoto_updatesNothing() {
        // given
        val photoSeq = 999L
        every { photosRepository.updateActive(photoSeq, false) } returns 0

        // when
        sut.deletePhoto(photoSeq)

        // then
        verify(exactly = 1) { photosRepository.updateActive(photoSeq, false) }
    }
}
