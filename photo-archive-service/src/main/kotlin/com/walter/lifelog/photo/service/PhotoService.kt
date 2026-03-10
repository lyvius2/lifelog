package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.mapper.PhotoMapper
import com.walter.lifelog.photo.repository.PhotoCategoryRepository
import com.walter.lifelog.photo.repository.PhotoRepository
import com.walter.lifelog.photo.repository.PhotoTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val photoMapper: PhotoMapper,
    private val photoRepository: PhotoRepository,
    private val photoCategoryRepository: PhotoCategoryRepository,
    private val photoCategoryMapper: PhotoCategoryMapper,
    private val photoTagRepository: PhotoTagRepository,
) {
    @Transactional(readOnly = true)
    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        val categories = photoCategoryRepository.findAllActiveCategories()
        return photoCategoryMapper.toResponseList(categories)
    }

    @Transactional(readOnly = true)
    fun getTags(photoSeq: Long): List<String> {
        return photoTagRepository.findByPhotoSeq(photoSeq).map { it.tag }
    }

    @Transactional
    fun savePhoto(uploadRequest: UploadRequest, mainFileName: String, subFileName: String, userSeq: Long, folderPath: String) {
        val photoToSave = photoMapper.toEntity(uploadRequest, mainFileName, subFileName, userSeq, folderPath)
        val savedPhoto = photoRepository.save(photoToSave)
        val photoSeq = savedPhoto.photoSeq!!
        photoTagRepository.deleteByPhotoSeq(savedPhoto.userSeq)
        uploadRequest.tags?.forEachIndexed { index, tag ->
            photoTagRepository.save(PhotoTag(photoSeq, index, tag))
        }
    }
}