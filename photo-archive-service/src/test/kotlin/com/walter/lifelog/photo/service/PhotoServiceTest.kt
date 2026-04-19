package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.entity.Photo
import com.walter.lifelog.photo.entity.PhotoCategory
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoriesRepository
import com.walter.lifelog.photo.repository.PhotosQueryRepository
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor

class PhotoServiceTest {
    private lateinit var virtualThreadExecutor: TaskExecutor
    private lateinit var photoMapper: PhotoMapper
    private lateinit var photosRepository: PhotosRepository
    private lateinit var photoCategoriesRepository: PhotoCategoriesRepository
    private lateinit var photoCategoryMapper: PhotoCategoryMapper
    private lateinit var photoTagsRepository: PhotoTagsRepository
    private lateinit var photosQueryRepository: PhotosQueryRepository
    private lateinit var photoService: PhotoService

    @BeforeEach
    fun setUp() {
        virtualThreadExecutor = mockk()
        photoMapper = mockk()
        photosRepository = mockk()
        photoCategoriesRepository = mockk()
        photoCategoryMapper = mockk()
        photoTagsRepository = mockk()
        photosQueryRepository = mockk()
        photoService = PhotoService(
            virtualThreadExecutor,
            photoMapper,
            photosRepository,
            photoCategoriesRepository,
            photoCategoryMapper,
            photoTagsRepository,
            photosQueryRepository
        )
    }

    @Test
    @DisplayName("활성화된 포토 카테고리 목록을 조회하고 정확하게 매핑한다")
    fun getActivePhotoCategories() {
        // given
        val categories = listOf(
            PhotoCategory(categorySeq = 1L, categoryName = "My Car", icon = "🚗"),
            PhotoCategory(categorySeq = 2L, categoryName = "Nature", icon = "🌿"),
            PhotoCategory(categorySeq = 3L, categoryName = "Travel", icon = "✈️"),
        )
        val expectedResponses = listOf(
            PhotoCategoryResponse(categorySeq = 1L, icon = "🚗", name = "My Car"),
            PhotoCategoryResponse(categorySeq = 2L, icon = "🌿", name = "Nature"),
            PhotoCategoryResponse(categorySeq = 3L, icon = "✈️", name = "Travel"),
        )

        every { photoCategoriesRepository.findAllActiveCategories() } returns categories
        every { photoCategoryMapper.toResponseList(categories) } returns expectedResponses

        // when
        val result = photoService.getActivePhotoCategories()

        // then
        assertEquals(3, result.size)
        assertEquals(1L, result[0].categorySeq)
        assertEquals("🚗", result[0].icon)
        assertEquals("My Car", result[0].name)
        assertEquals(2L, result[1].categorySeq)
        assertEquals("🌿", result[1].icon)
        assertEquals("Nature", result[1].name)
        assertEquals(3L, result[2].categorySeq)
        assertEquals("✈️", result[2].icon)
        assertEquals("Travel", result[2].name)

        verify(exactly = 1) { photoCategoriesRepository.findAllActiveCategories() }
        verify(exactly = 1) { photoCategoryMapper.toResponseList(categories) }
    }

    @Test
    @DisplayName("활성화된 포토 카테고리가 없으면 빈 리스트를 반환한다")
    fun getActivePhotoCategoriesEmpty() {
        // given
        every { photoCategoriesRepository.findAllActiveCategories() } returns emptyList()
        every { photoCategoryMapper.toResponseList(emptyList()) } returns emptyList()

        // when
        val result = photoService.getActivePhotoCategories()

        // then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { photoCategoriesRepository.findAllActiveCategories() }
        verify(exactly = 1) { photoCategoryMapper.toResponseList(emptyList()) }
    }

    @Test
    @DisplayName("updateLikeCount - 존재하는 사진의 좋아요 수를 정상적으로 업데이트한다")
    fun updateLikeCountSuccess() {
        // given
        val photoSeq = 1L
        val newLikeCount = 10
        val photo = Photo(
            photoSeq = photoSeq,
            userSeq = 1L,
            title = "Test Photo",
            imageUrl = "/lifelog/test.jpg",
            likeCount = 5
        )

        every { photosRepository.findTopByPhotoSeq(photoSeq) } returns photo
        every { photosRepository.save(any<Photo>()) } returns photo

        // when
        photoService.updateLikeCount(photoSeq, newLikeCount)

        // then
        assertEquals(newLikeCount, photo.likeCount)
        verify(exactly = 1) { photosRepository.findTopByPhotoSeq(photoSeq) }
        verify(exactly = 1) { photosRepository.save(photo) }
    }

    @Test
    @DisplayName("updateLikeCount - 존재하지 않는 사진이면 아무 작업도 하지 않는다")
    fun updateLikeCountPhotoNotFound() {
        // given
        val photoSeq = 999L

        every { photosRepository.findTopByPhotoSeq(photoSeq) } returns null

        // when
        photoService.updateLikeCount(photoSeq, 10)

        // then
        verify(exactly = 1) { photosRepository.findTopByPhotoSeq(photoSeq) }
        verify(exactly = 0) { photosRepository.save(any<Photo>()) }
    }

    @Test
    @DisplayName("deletePhoto - 존재하는 사진의 is_active를 false로 업데이트한다")
    fun deletePhotoSuccess() {
        // given
        val photoSeq = 1L
        val photo = Photo(
            photoSeq = photoSeq,
            userSeq = 1L,
            title = "삭제할 사진",
            imageUrl = "/lifelog/test.jpg",
            isActive = true
        )

        every { photosRepository.findTopByPhotoSeq(photoSeq) } returns photo
        every { photosRepository.save(any<Photo>()) } returns photo

        // when
        photoService.deletePhoto(photoSeq)

        // then
        assertEquals(false, photo.isActive)
        verify(exactly = 1) { photosRepository.findTopByPhotoSeq(photoSeq) }
        verify(exactly = 1) { photosRepository.save(photo) }
    }

    @Test
    @DisplayName("deletePhoto - 존재하지 않는 사진이면 아무 작업도 하지 않는다")
    fun deletePhotoNotFound() {
        // given
        val photoSeq = 999L

        every { photosRepository.findTopByPhotoSeq(photoSeq) } returns null

        // when
        photoService.deletePhoto(photoSeq)

        // then
        verify(exactly = 1) { photosRepository.findTopByPhotoSeq(photoSeq) }
        verify(exactly = 0) { photosRepository.save(any<Photo>()) }
    }
}
