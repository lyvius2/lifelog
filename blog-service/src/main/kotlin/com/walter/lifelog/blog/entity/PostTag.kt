package com.walter.lifelog.blog.entity

import java.time.LocalDateTime

/**
 * posts_tags 테이블의 한 행. (post_seq, tag_seq) 복합 키입니다.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.blog.repository.PostTagsRepository] 가 jOOQ 로 수행합니다.
 */
data class PostTag(
    val postSeq: Long,
    val tagSeq: Int,
    val tag: String,
    val createdAt: LocalDateTime? = null,
)
