package com.walter.lifelog.user.entity

import java.time.LocalDateTime

/**
 * users 테이블의 한 행.
 *
 * JPA 엔티티가 아닙니다. 매핑은
 * [com.walter.lifelog.user.repository.UserRepository] 가 jOOQ 로 수행합니다.
 */
data class User(
    val userSeq: Long? = null,
    val email: String,
    val name: String,
    val passwordHash: String,
    val displayName: String,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val githubUrl: String? = null,
    val linkedinUrl: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
)
