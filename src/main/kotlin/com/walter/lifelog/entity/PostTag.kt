package com.walter.lifelog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime

data class PostTagId(
    val postSeq: Long = 0,
    val tagSeq: Int = 0
) : Serializable

@Entity
@Table(name = "posts_tags")
@IdClass(PostTagId::class)
data class PostTag(
    @Id
    @Column(name = "post_seq", nullable = false)
    val postSeq: Long,

    @Id
    @Column(name = "tag_seq", nullable = false)
    val tagSeq: Int,

    @Column(name = "tag", nullable = false, length = 100)
    val tag: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)

