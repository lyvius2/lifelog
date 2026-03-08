package com.walter.lifelog.photo.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime

data class PhotoTagId(
    val photoSeq: Long = 0,
    val tagSeq: Int = 0
) : Serializable

@Entity
@Table(
    name = "photos_tags",
    indexes = [
        Index(name = "idx_tag", columnList = "tag")
    ]
)
@IdClass(PhotoTagId::class)
data class PhotoTag(
    @Id
    @Column(name = "photo_seq")
    val photoSeq: Long,

    @Id
    @Column(name = "tag_seq")
    val tagSeq: Int,

    @Column(name = "tag", nullable = false, length = 100)
    val tag: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)

