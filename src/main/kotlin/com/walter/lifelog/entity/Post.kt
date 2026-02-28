package com.walter.lifelog.entity

import com.walter.lifelog.entity.code.PostStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_slug", columnList = "slug"),
        Index(name = "idx_status", columnList = "status"),
        Index(name = "idx_published_at", columnList = "published_at"),
        Index(name = "idx_user_seq", columnList = "user_seq"),
        Index(name = "idx_category_seq", columnList = "category_seq"),
        Index(name = "idx_featured", columnList = "is_featured")
    ]
)
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_seq")
    val postSeq: Long? = null,

    @Column(name = "user_seq", nullable = false)
    val userSeq: Long,

    @Column(name = "category_seq")
    val categorySeq: Long? = null,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    val slug: String,

    @Column(name = "summary", columnDefinition = "TEXT")
    val summary: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    val content: String,

    @Column(name = "markdown_content", columnDefinition = "LONGTEXT")
    val markdownContent: String?,

    @Column(name = "thumbnail_url", length = 500)
    val thumbnailUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: PostStatus = PostStatus.DRAFT,

    @Column(name = "view_count", nullable = false)
    val viewCount: Int = 0,

    @Column(name = "published_at")
    val publishedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
