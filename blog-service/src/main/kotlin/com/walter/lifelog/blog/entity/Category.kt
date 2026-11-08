package com.walter.lifelog.blog.entity

import java.time.LocalDateTime

/**
 * categories 테이블의 한 행.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.blog.repository.CategoriesRepository] 가 jOOQ 로 수행합니다.
 */
data class Category(
    val categorySeq: Long? = null,
    val categoryName: String,
    val slug: String,
    val description: String? = null,
    val parentCategoryId: Long? = null,
    val displayOrder: Int = 0,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
)
