package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoriesRepository
import com.walter.lifelog.photo.repository.PhotosQueryRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.shared.paging.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val photoMapper: PhotoMapper,
    private val photosRepository: PhotosRepository,
    private val photoCategoriesRepository: PhotoCategoriesRepository,
    private val photoCategoryMapper: PhotoCategoryMapper,
    private val photoTagsRepository: PhotoTagsRepository,
    private val photosQueryRepository: PhotosQueryRepository,
) {
    @Transactional(readOnly = true)
    fun getPhotos(photoSearchRequest: PhotoSearchRequest): PageResponse<PhotoSearchResponse> {
        return photosQueryRepository.findPhotos(photoSearchRequest)
    }

    @Transactional
    fun savePhoto(uploadRequest: UploadRequest, mainFileName: String, subFileName: String, userSeq: Long, folderPath: String) {
        val photoToSave = photoMapper.toEntity(uploadRequest, mainFileName, subFileName, userSeq, folderPath)
        val savedPhoto = photosRepository.save(photoToSave)
        val photoSeq = savedPhoto.photoSeq!!
        photoTagsRepository.deleteByPhotoSeq(savedPhoto.userSeq)
        uploadRequest.tags?.forEachIndexed { index, tag ->
            photoTagsRepository.save(PhotoTag(photoSeq, index, tag))
        }
    }

    @Transactional(readOnly = true)
    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        val categories = photoCategoriesRepository.findAllActiveCategories()
        return photoCategoryMapper.toResponseList(categories)
    }

    @Transactional(readOnly = true)
    fun getTags(photoSeq: Long): List<String> {
        return photoTagsRepository.findByPhotoSeq(photoSeq).map { it.tag }
    }
}