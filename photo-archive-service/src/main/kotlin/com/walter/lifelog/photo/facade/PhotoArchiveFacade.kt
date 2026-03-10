package com.walter.lifelog.photo.facade

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.dto.UploadResponse
import com.walter.lifelog.photo.service.GoogleDriveService
import com.walter.lifelog.photo.service.PhotoService
import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.paging.PageResponse
import org.springframework.web.multipart.MultipartFile

@Facade
class PhotoArchiveFacade(
    private val googleDriveService: GoogleDriveService,
    private val photoService: PhotoService,
) {
    fun getPhotos(categorySeq: Long?, page: Int?): PageResponse<PhotoSearchResponse> {
        val page = page?.coerceAtLeast(1) ?: 1
        val photoSearchRequest = PhotoSearchRequest(categorySeq = categorySeq, page = page)
        return photoService.getPhotos(photoSearchRequest)
    }

    fun uploadPhoto(uploadRequest: UploadRequest, folderPath: String, uploaderUserSeq: Long, file: MultipartFile): UploadResponse {
        val files = googleDriveService.uploadImage(folderPath, file)
        photoService.savePhoto(uploadRequest, files.first.name, files.second.name, uploaderUserSeq, folderPath)
        return UploadResponse.of(files.first, folderPath)
    }

    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        return photoService.getActivePhotoCategories()
    }
}