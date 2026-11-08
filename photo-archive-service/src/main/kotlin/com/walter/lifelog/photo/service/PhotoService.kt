package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoShotPeriod
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoriesRepository
import com.walter.lifelog.photo.repository.PhotosQueryRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.paging.PageResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val photoMapper: PhotoMapper,
    private val photosRepository: PhotosRepository,
    private val photoCategoriesRepository: PhotoCategoriesRepository,
    private val photoTagsRepository: PhotoTagsRepository,
    private val photosQueryRepository: PhotosQueryRepository,
) {
    @DynamicCacheable(value = ["photos"], key = "#photoSearchRequest.toString()", ttlMinutes = 1)
    fun getPhotos(photoSearchRequest: PhotoSearchRequest): PageResponse<PhotoSearchResponse> {
        return photosQueryRepository.findPhotos(photoSearchRequest)
    }

    @Transactional
    fun savePhoto(uploadRequest: UploadRequest, mainFileName: String, subFileName: String, userSeq: Long, folderPath: String) {
        val photoToSave = photoMapper.toEntity(uploadRequest, mainFileName, subFileName, userSeq, folderPath)
        val photoSeq = photosRepository.insert(photoToSave)
        photoTagsRepository.deleteByPhotoSeq(photoSeq)
        uploadRequest.tags?.mapIndexed { index, tag -> PhotoTag(photoSeq, index, tag) }
            ?.let { photoTagsRepository.saveAll(it) }
    }

    @DynamicCacheable(value = ["activePhotoCategories"], key = "'activeCategories'", ttlMinutes = 60)
    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        return photoCategoriesRepository.findActiveCategoryResponses()
    }

    @DynamicCacheable(value = ["photoShotPeriod"], key = "'photoShotPeriod'", ttlMinutes = 60)
    fun getPhotoShotPeriod(): PhotoShotPeriod = photosRepository.findShotPeriod()

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["photos"], allEntries = true),
        CacheEvict(value = ["activePhotos"], allEntries = true),
    ])
    fun deletePhoto(photoSeq: Long) {
        photosRepository.updateActive(photoSeq, false)
    }

    @Transactional
    fun updateLikeCount(photoSeq: Long, likeCount: Int) {
        photosRepository.updateLikeCount(photoSeq, likeCount)
    }
}