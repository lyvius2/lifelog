package com.walter.lifelog.photo.entity

import com.walter.lifelog.photo.entity.code.PhotoStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "photos",
    indexes = [
        Index(name = "idx_user_seq", columnList = "user_seq"),
        Index(name = "idx_category_seq", columnList = "category_seq"),
        Index(name = "idx_created_at", columnList = "created_at")
    ]
)
data class Photo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_seq")
    val photoSeq: Long? = null,

    @Column(name = "user_seq", nullable = false)
    val userSeq: Long,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "caption", columnDefinition = "TEXT")
    val caption: String? = null,

    @Column(name = "category_seq")
    val categorySeq: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_seq", insertable = false, updatable = false)
    val category: PhotoCategory? = null,

    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,

    @Column(name = "thumbnail_url", length = 500)
    val thumbnailUrl: String? = null,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(name = "exif_maker", length = 100)
    val exifMaker: String? = null,

    @Column(name = "exif_model", length = 100)
    val exifModel: String? = null,

    @Column(name = "exif_aperture", length = 20)
    val exifAperture: String? = null,

    @Column(name = "exif_shutter", length = 20)
    val exifShutter: String? = null,

    @Column(name = "exif_iso", length = 20)
    val exifIso: String? = null,

    @Column(name = "exif_focal_length")
    val exifFocalLength: Long? = null,

    @Column(name = "exif_lens", length = 200)
    val exifLens: String? = null,

    @Column(name = "exif_flash", length = 3)
    val exifFlash: String? = null,

    @Column(name = "gps_latitude")
    val gpsLatitude: BigDecimal? = null,

    @Column(name = "gps_longitude")
    val gpsLongitude: BigDecimal? = null,

    @Column(name = "shot_at")
    val shotAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "status", nullable = false)
    val status: PhotoStatus = PhotoStatus.UPLOADED,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,
)

