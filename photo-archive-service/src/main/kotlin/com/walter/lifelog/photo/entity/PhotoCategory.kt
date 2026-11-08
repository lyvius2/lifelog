package com.walter.lifelog.photo.entity

import java.time.LocalDateTime

/**
 * photos_categories 테이블의 한 행.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.photo.repository.PhotoCategoriesRepository] 가 jOOQ 로 수행합니다.
 */
data class PhotoCategory(
    val categorySeq: Long? = null,
    val categoryName: String,
    val icon: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
