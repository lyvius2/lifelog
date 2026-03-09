package com.walter.lifelog.photo.service

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.mapper.PhotoCategoryMapper
import com.walter.lifelog.photo.repository.PhotoCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val photoCategoryRepository: PhotoCategoryRepository,
    private val photoCategoryMapper: PhotoCategoryMapper,
) {
    @Transactional(readOnly = true)
    fun getActivePhotoCategories(): List<PhotoCategoryResponse> {
        val categories = photoCategoryRepository.findAllActiveCategories()
        return photoCategoryMapper.toResponseList(categories)
    }
}