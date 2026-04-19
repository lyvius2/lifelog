package com.walter.lifelog.photo.facade

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.service.GoogleDriveService
import com.walter.lifelog.photo.service.PhotoService
import com.walter.lifelog.shared.paging.PageResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.web.multipart.MultipartFile
import com.walter.lifelog.shared.util.ViewCountHelper

@DisplayName("PhotoArchiveFacade 테스트")
class PhotoArchiveFacadeTest {

    private val googleDriveService: GoogleDriveService = mockk()
    private val photoService: PhotoService = mockk(relaxed = true)
    private val viewCountHelper: ViewCountHelper = mockk(relaxed = true)
    private val virtualThreadExecutor: TaskExecutor = mockk(relaxed = true)
    private val facade = PhotoArchiveFacade(googleDriveService, photoService, viewCountHelper, virtualThreadExecutor)

    @Test
    @DisplayName("getPhotos - categorySeq와 page를 PhotoSearchRequest로 변환하여 조회한다")
    fun getPhotos_shouldDelegateToPhotoService() {
        // given
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 24, 0L, 0)
        val requestSlot = slot<PhotoSearchRequest>()
        every { photoService.getPhotos(capture(requestSlot)) } returns expectedResponse

        // when
        val result = facade.getPhotos(categorySeq = 3L, page = 2)

        // then
        assertThat(result).isEqualTo(expectedResponse)
        assertThat(requestSlot.captured.categorySeq).isEqualTo(3L)
        assertThat(requestSlot.captured.page).isEqualTo(2)
        verify { photoService.getPhotos(any()) }
    }

    @Test
    @DisplayName("getPhotos - page가 null이면 1로 설정한다")
    fun getPhotos_shouldDefaultPageToOneWhenNull() {
        // given
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 24, 0L, 0)
        val requestSlot = slot<PhotoSearchRequest>()
        every { photoService.getPhotos(capture(requestSlot)) } returns expectedResponse

        // when
        facade.getPhotos(categorySeq = null, page = null)

        // then
        assertThat(requestSlot.captured.categorySeq).isNull()
        assertThat(requestSlot.captured.page).isEqualTo(1)
    }

    @Test
    @DisplayName("getPhotos - page가 0 이하이면 1로 보정한다")
    fun getPhotos_shouldCoercePageToAtLeastOne() {
        // given
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 24, 0L, 0)
        val requestSlot = slot<PhotoSearchRequest>()
        every { photoService.getPhotos(capture(requestSlot)) } returns expectedResponse

        // when
        facade.getPhotos(categorySeq = null, page = -5)

        // then
        assertThat(requestSlot.captured.page).isEqualTo(1)
    }

    @Test
    @DisplayName("uploadPhoto - Google Drive에 업로드 후 DB에 저장하고 UploadResponse를 반환한다")
    fun uploadPhoto_shouldUploadAndSave() {
        // given
        val uploadRequest = UploadRequest(
            title = "벚꽃 사진",
            caption = "봄날의 풍경",
            categorySeq = 1L,
            tags = listOf("#봄", "#벚꽃"),
            maker = "SONY", model = "A7IV", lens = null,
            aperture = "f/2.8", shutter = "1/250s", iso = "400",
            focalLength = 50L, flash = "Off",
            latitude = null, longitude = null, shotAt = null
        )
        val folderPath = "lifelog/photos"
        val uploaderUserSeq = 1L
        val multipartFile: MultipartFile = mockk()

        val mainFile: File = mockk {
            every { id } returns "mainFileId123"
            every { name } returns "cherry_blossom.jpg"
            every { mimeType } returns "image/jpeg"
            every { getSize() } returns 2048576L
            every { webViewLink } returns "https://drive.google.com/view/mainFileId123"
            every { webContentLink } returns "https://drive.google.com/download/mainFileId123"
        }
        val thumbFile: File = mockk {
            every { id } returns "thumbFileId456"
            every { name } returns "cherry_blossom_thumb.jpg"
            every { mimeType } returns "image/jpeg"
            every { getSize() } returns 102400L
        }
        every { googleDriveService.uploadImage(folderPath, multipartFile) } returns Pair(mainFile, thumbFile)

        // when
        val result = facade.uploadPhoto(uploadRequest, folderPath, uploaderUserSeq, multipartFile)

        // then
        assertThat(result.fileId).isEqualTo("mainFileId123")
        assertThat(result.fileName).isEqualTo("cherry_blossom.jpg")
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.fileSize).isEqualTo(2048576L)
        assertThat(result.drivePath).isEqualTo("lifelog/photos/cherry_blossom.jpg")
        assertThat(result.webViewLink).isEqualTo("https://drive.google.com/view/mainFileId123")

        verify { googleDriveService.uploadImage(folderPath, multipartFile) }
        verify { photoService.savePhoto(uploadRequest, "cherry_blossom.jpg", "cherry_blossom_thumb.jpg", 1L, folderPath) }
    }

    @Test
    @DisplayName("getActivePhotoCategories - 활성 카테고리 목록을 반환한다")
    fun getActivePhotoCategories_shouldReturnCategoryList() {
        // given
        val categories = listOf(
            PhotoCategoryResponse(1L, "🚗", "My Car"),
            PhotoCategoryResponse(2L, "✈️", "Travel"),
            PhotoCategoryResponse(3L, "🌿", "Nature")
        )
        every { photoService.getActivePhotoCategories() } returns categories

        // when
        val result = facade.getActivePhotoCategories()

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].categorySeq).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("My Car")
        assertThat(result[0].icon).isEqualTo("🚗")
        assertThat(result[1].name).isEqualTo("Travel")
        assertThat(result[2].name).isEqualTo("Nature")
        verify { photoService.getActivePhotoCategories() }
    }

    @Test
    @DisplayName("getActivePhotoCategories - 카테고리가 없으면 빈 리스트를 반환한다")
    fun getActivePhotoCategories_shouldReturnEmptyListWhenNone() {
        // given
        every { photoService.getActivePhotoCategories() } returns emptyList()

        // when
        val result = facade.getActivePhotoCategories()

        // then
        assertThat(result).isEmpty()
        verify { photoService.getActivePhotoCategories() }
    }

    @Test
    @DisplayName("deletePhoto - photoService.deletePhoto를 호출한다")
    fun deletePhoto_shouldDelegateToPhotoService() {
        // given
        val photoSeq = 5L
        every { photoService.deletePhoto(photoSeq) } returns Unit

        // when
        facade.deletePhoto(photoSeq)

        // then
        verify(exactly = 1) { photoService.deletePhoto(photoSeq) }
    }
}

