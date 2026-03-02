package com.walter.lifelog.service

import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.repository.CategoriesRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoriesRepository: CategoriesRepository,
    private val categoryMapper: CategoryMapper,
) {
    fun getActiveCategories() = categoriesRepository.findInActive().map { categoryMapper.toPostInputCategory(it) }
}