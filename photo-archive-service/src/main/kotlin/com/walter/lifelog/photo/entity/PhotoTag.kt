package com.walter.lifelog.photo.entity

import java.time.LocalDateTime

/**
 * photos_tags 테이블의 한 행. (photo_seq, tag_seq) 복합 키입니다.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.photo.repository.PhotoTagsRepository] 가 jOOQ 로 수행합니다.
 */
data class PhotoTag(
    val photoSeq: Long,
    val tagSeq: Int,
    val tag: String,
    val createdAt: LocalDateTime? = null,
)
