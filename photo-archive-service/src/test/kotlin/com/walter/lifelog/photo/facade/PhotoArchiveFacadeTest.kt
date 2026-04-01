package com.walter.lifelog.photo.facade

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoShotPeriod
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.service.PhotoService
import com.walter.lifelog.shared.service.GoogleDriveService
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.ViewCountHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

@DisplayName("PhotoArchiveFacade 테스트")
class PhotoArchiveFacadeTest {

    private val photoService: PhotoService = mockk(relaxed = true)
    private val googleDriveService: GoogleDriveService = mockk()
    private val viewCountHelper: ViewCountHelper = mockk()
    private val virtualThreadExecutor: TaskExecutor = TaskExecutor { task ->
        Executors.newVirtualThreadPerTaskExecutor().execute(task)
    }

    private val facade = PhotoArchiveFacade(
        photoService = photoService,
        googleDriveService = googleDriveService,
        viewCountHelper = viewCountHelper,
        virtualThreadExecutor = virtualThreadExecutor,
    )

    @Test
    @DisplayName("getPhotos - categorySeq와 page를 PhotoSearchRequest로 변환하여 조회한다")
    fun getPhotos_shouldDelegateToPhotoService() {
        // given
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 12, 0L, 0)
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
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 12, 0L, 0)
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
        val expectedResponse = PageResponse(emptyList<PhotoSearchResponse>(), 1, 12, 0L, 0)
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

        val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val multipartFile: MultipartFile = mockk()
        every { multipartFile.isEmpty } returns false
        every { multipartFile.contentType } returns "image/jpeg"
        every { multipartFile.originalFilename } returns "cherry_blossom.jpg"
        every { multipartFile.inputStream } returns inputStream

        val driveFile = File().apply {
            id = "mainFileId123"
            name = "cherry_blossom.jpg"
            mimeType = "image/jpeg"
            setSize(2048576L)
            webViewLink = "https://drive.google.com/view/mainFileId123"
            webContentLink = "https://drive.google.com/download/mainFileId123"
        }
        every { googleDriveService.uploadImage(folderPath, "cherry_blossom.jpg", "image/jpeg", inputStream) } returns driveFile

        // when
        val result = facade.uploadPhoto(uploadRequest, folderPath, uploaderUserSeq, multipartFile)

        // then
        assertThat(result.fileId).isEqualTo("mainFileId123")
        assertThat(result.fileName).isEqualTo("cherry_blossom.jpg")
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.fileSize).isEqualTo(2048576L)
        assertThat(result.drivePath).isEqualTo("lifelog/photos/cherry_blossom.jpg")
        assertThat(result.webViewLink).isEqualTo("https://drive.google.com/view/mainFileId123")

        verify { googleDriveService.uploadImage(folderPath, "cherry_blossom.jpg", "image/jpeg", inputStream) }
        verify { photoService.savePhoto(uploadRequest, "cherry_blossom.jpg", 1L, folderPath) }
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
    @DisplayName("getPhotoArchiveViewInfo - 카테고리, 사진 목록, 촬영 기간을 병렬 조회하여 반환한다")
    fun getPhotoArchiveViewInfo_shouldReturnPhotoArchive() {
        // given
        val categories = listOf(PhotoCategoryResponse(1L, "🚗", "My Car"))
        val archive = PageResponse(emptyList<PhotoSearchResponse>(), 1, 12, 0L, 0)
        val period = PhotoShotPeriod(minYear = 2020, maxYear = 2026)
        every { photoService.getActivePhotoCategories() } returns categories
        every { photoService.getPhotos(any<PhotoSearchRequest>()) } returns archive
        every { photoService.getPhotoShotPeriod() } returns period

        // when
        val result = facade.getPhotoArchiveViewInfo()

        // then
        assertThat(result.categories).isEqualTo(categories)
        assertThat(result.archive).isEqualTo(archive)
        assertThat(result.period.minYear).isEqualTo(2020)
        assertThat(result.period.maxYear).isEqualTo(2026)
    }

    @Test
    @DisplayName("increaseLikeCount - Redis 좋아요 수를 증가시키고 DB를 업데이트한다")
    fun increaseLikeCount_shouldIncrementAndReturnLikeCount() {
        // given
        every { viewCountHelper.increment("photo_like_7") } returns 42L

        // when
        val result = facade.increaseLikeCount(7L)

        // then
        assertThat(result.photoSeq).isEqualTo(7L)
        assertThat(result.likeCount).isEqualTo(42)
        verify { viewCountHelper.increment("photo_like_7") }
        verify { photoService.updateLikeCount(7L, 42) }
    }
}
