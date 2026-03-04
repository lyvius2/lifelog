package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.CategoryTreeResponse
import com.walter.lifelog.blog.entity.Category
import com.walter.lifelog.blog.mapper.CategoryMapper
import com.walter.lifelog.blog.repository.CategoriesRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoriesRepository: CategoriesRepository,
    private val categoryMapper: CategoryMapper,
) {
    companion object {
        private const val MAX_DEPTH = 3
    }

    fun getActiveCategories() = categoriesRepository.findInActive().map { categoryMapper.toPostInputCategory(it) }

    fun getCategoryTree(): List<CategoryTreeResponse> {
        val allCategories = categoriesRepository.findInActive()
        val childrenMap = allCategories.groupBy { it.parentCategoryId }
        val roots = childrenMap[null] ?: return emptyList()
        return roots
            .sortedBy { it.displayOrder }
            .map { buildNode(it, childrenMap, 1) }
    }

    private fun buildNode(category: Category, childrenMap: Map<Long?, List<Category>>, currentDepth: Int): CategoryTreeResponse {
        val children = if (currentDepth < MAX_DEPTH) {
            (childrenMap[category.categorySeq] ?: emptyList())
                .sortedBy { it.displayOrder }
                .map { buildNode(it, childrenMap, currentDepth + 1) }
        } else {
            emptyList()
        }

        return CategoryTreeResponse.from(category, currentDepth, children)
    }
}