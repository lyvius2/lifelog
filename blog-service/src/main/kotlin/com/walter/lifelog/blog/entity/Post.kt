package com.walter.lifelog.blog.entity

import com.walter.lifelog.blog.entity.code.PostStatus
import java.time.LocalDateTime

/**
 * posts 테이블의 한 행.
 *
 * JPA 엔티티가 아닙니다. 매핑은 [com.walter.lifelog.blog.repository.PostsRepository] 가
 * jOOQ 로 직접 수행합니다. 컬럼명과 필드명의 대응은 그 클래스의 companion object 에
 * 모여 있습니다.
 *
 * [category] 는 조회 쿼리가 categories 를 조인해 채웁니다. 채우지 않는 조회에서는
 * null 입니다 (예전 `@EntityGraph` 가 하던 일을 쿼리가 명시적으로 합니다).
 */
data class Post(
    val postSeq: Long? = null,
    val userSeq: Long,
    val categorySeq: Long? = null,
    val category: Category? = null,
    val title: String,
    val slug: String? = null,
    val summary: String? = null,
    val content: String,
    val markdownContent: String?,
    val thumbnailUrl: String? = null,
    val status: PostStatus = PostStatus.DRAFT,
    var viewCount: Int = 0,
    var publishedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
