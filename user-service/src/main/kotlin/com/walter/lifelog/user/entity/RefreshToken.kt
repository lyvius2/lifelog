package com.walter.lifelog.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_user_seq", columnList = "user_seq"),
        Index(name = "idx_token", columnList = "token"),
        Index(name = "idx_expires_at", columnList = "expires_at")
    ]
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_seq")
    val refreshTokenSeq: Long? = null,

    @Column(name = "user_seq", nullable = false)
    val userSeq: Long,

    @Column(name = "token", nullable = false, unique = true, length = 500)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)

