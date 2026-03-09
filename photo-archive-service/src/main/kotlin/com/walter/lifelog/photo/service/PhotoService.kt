package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.repository.PhotoCategoryRepository
import com.walter.lifelog.photo.repository.PhotoTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
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
    fun saveTags(photoSeq: Long, tags: List<String>?) {
        photoTagRepository.deleteByPhotoSeq(photoSeq)
        tags?.forEachIndexed { index, tag ->
            photoTagRepository.save(PhotoTag(photoSeq = photoSeq, tagSeq = index, tag = tag))
        }
    }
}