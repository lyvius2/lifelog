package com.walter.lifelog.blog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "categories",
    indexes = [
        Index(name = "idx_slug", columnList = "slug"),
        Index(name = "idx_parent", columnList = "parent_category_id")
    ]
)
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_seq")
    val categorySeq: Long? = null,

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    val categoryName: String,

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    val slug: String,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "parent_category_id")
    val parentCategoryId: Long? = null,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)