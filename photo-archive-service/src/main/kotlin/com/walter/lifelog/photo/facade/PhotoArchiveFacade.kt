package com.walter.lifelog.photo.facade

import com.walter.lifelog.photo.dto.PhotoArchive
import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoLikeCountResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.dto.UploadResponse
import com.walter.lifelog.photo.service.PhotoService
import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.service.GoogleDriveService
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import com.walter.lifelog.shared.util.ViewCountHelper
import org.springframework.core.task.TaskExecutor
import org.springframework.web.multipart.MultipartFile

@Facade
class PhotoArchiveFacade(
    private val photoService: PhotoService,
    private val googleDriveService: GoogleDriveService,
    private val viewCountHelper: ViewCountHelper,
    private val virtualThreadExecutor: TaskExecutor,
) {
    fun getPhotos(categorySeq: Long?, page: Int?): PageResponse<PhotoSearchResponse> {
        val page = page?.coerceAtLeast(1) ?: 1
        val photoSearchRequest = PhotoSearchRequest(categorySeq = categorySeq, page = page)
        return photoService.getPhotos(photoSearchRequest)
    }

    fun getPhotoArchiveViewInfo(): PhotoArchive {
        val categoriesFuture = asyncSupply(virtualThreadExecutor) { photoService.getActivePhotoCategories() }
        val archiveFuture = asyncSupply(virtualThreadExecutor) { photoService.getPhotos(PhotoSearchRequest()) }
        val periodFuture = asyncSupply(virtualThreadExecutor) { photoService.getPhotoShotPeriod() }
        return PhotoArchive(
            categories = categoriesFuture.get(),
            archive = archiveFuture.get(),
            period = periodFuture.get(),
        )
    }

    fun uploadPhoto(uploadRequest: UploadRequest, folderPath: String, uploaderUserSeq: Long, file: MultipartFile): UploadResponse {
        require(!file.isEmpty) { "file is empty" }
        val contentType = file.contentType
        require(contentType != null && contentType.startsWith("image/")) { "Only image files can be uploaded : $contentType" }
        val file = googleDriveService.uploadImage(folderPath, file.originalFilename, contentType, file.inputStream)
        photoService.savePhoto(uploadRequest, file.name, uploaderUserSeq, folderPath)
        return UploadResponse.of(file, folderPath)
    }

    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        return photoService.getActivePhotoCategories()
    }

    fun increaseLikeCount(photoSeq: Long): PhotoLikeCountResponse {
        val likeCount = viewCountHelper.increment("photo_like_$photoSeq").toInt()
        asyncSupply(virtualThreadExecutor) { photoService.updateLikeCount(photoSeq, likeCount) }
        return PhotoLikeCountResponse(photoSeq, likeCount)
    }
}