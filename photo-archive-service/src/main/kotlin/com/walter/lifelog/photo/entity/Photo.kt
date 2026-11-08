package com.walter.lifelog.photo.entity

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * photos 테이블의 한 행.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.photo.repository.PhotosRepository] 가 jOOQ 로 수행합니다.
 *
 * [category] 는 조회 쿼리가 photos_categories 를 조인해 채웁니다. 채우지 않는
 * 조회에서는 null 입니다.
 */
data class Photo(
    val photoSeq: Long? = null,
    val userSeq: Long,
    val title: String,
    val caption: String? = null,
    val categorySeq: Long? = null,
    val category: PhotoCategory? = null,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    var likeCount: Int = 0,
    val exifMaker: String? = null,
    val exifModel: String? = null,
    val exifAperture: String? = null,
    val exifShutter: String? = null,
    val exifIso: String? = null,
    val exifFocalLength: Long? = null,
    val exifLens: String? = null,
    val exifFlash: String? = null,
    val gpsLatitude: BigDecimal? = null,
    val gpsLongitude: BigDecimal? = null,
    val shotAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    var isActive: Boolean = true,
)
