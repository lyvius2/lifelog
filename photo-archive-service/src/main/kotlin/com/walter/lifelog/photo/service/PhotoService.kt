package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoShotPeriod
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoriesRepository
import com.walter.lifelog.photo.repository.PhotosQueryRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val virtualThreadExecutor: TaskExecutor,
    private val photoMapper: PhotoMapper,
    private val photosRepository: PhotosRepository,
    private val photoCategoriesRepository: PhotoCategoriesRepository,
    private val photoCategoryMapper: PhotoCategoryMapper,
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
        val savedPhoto = photosRepository.save(photoToSave)
        val photoSeq = savedPhoto.photoSeq!!
        photoTagsRepository.deleteByPhotoSeq(savedPhoto.userSeq)
        uploadRequest.tags?.mapIndexed { index, tag -> PhotoTag(photoSeq, index, tag) }
            ?.let { photoTagsRepository.saveAll(it) }
    }

    @DynamicCacheable(value = ["activePhotoCategories"], key = "'activeCategories'", ttlMinutes = 60)
    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        val categories = photoCategoriesRepository.findAllActiveCategories()
        return photoCategoryMapper.toResponseList(categories)
    }

    @DynamicCacheable(value = ["photoShotPeriod"], key = "'photoShotPeriod'", ttlMinutes = 60)
    fun getPhotoShotPeriod(): PhotoShotPeriod {
        val minYearFuture = asyncSupply(virtualThreadExecutor) { photosRepository.findEarliestShot()?.shotAt?.year }
        val maxYearFuture = asyncSupply(virtualThreadExecutor) { photosRepository.findLatestShot()?.shotAt?.year }
        return PhotoShotPeriod(
            minYear = minYearFuture.get(),
            maxYear = maxYearFuture.get(),
        )
    }

    @Transactional
    fun updateLikeCount(photoSeq: Long, likeCount: Int) {
        val photo = photosRepository.findTopByPhotoSeq(photoSeq)
        if (photo == null) {
            return
        }
        photo.likeCount = likeCount
        photosRepository.save(photo)
    }
}