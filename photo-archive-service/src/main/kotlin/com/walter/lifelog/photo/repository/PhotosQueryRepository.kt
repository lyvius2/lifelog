package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.dto.ExifInfo
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.shared.paging.PageResponse
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class PhotosQueryRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val PHOTOS = table("photos")
        private val PHOTOS_CATEGORIES = table("photos_categories")
        private val PHOTOS_TAGS = table("photos_tags")
        private val USERS = DSL.table("users")

        private val PHOTO_SEQ = field("photos.photo_seq", Long::class.java)
        private val IMAGE_URL = field("photos.image_url", String::class.java)
        private val THUMBNAIL_URL = field("photos.thumbnail_url", String::class.java)
        private val TITLE = field("photos.title", String::class.java)
        private val CAPTION = field("photos.caption", String::class.java)
        private val LIKE_COUNT = field("photos.like_count", Int::class.java)
        private val SHOT_AT = field("photos.shot_at", LocalDateTime::class.java)
        private val CREATED_AT = field("photos.created_at", LocalDateTime::class.java)
        private val IS_ACTIVE = field("photos.is_active", Boolean::class.java)
        private val USER_SEQ = DSL.field("photos.user_seq", Long::class.java)
        private val DISPLAY_NAME = DSL.field("users.display_name", String::class.java)
        private val PROFILE_IMAGE_URL = DSL.field("users.profile_image_url", String::class.java)
        private val EMAIL = DSL.field("users.email", String::class.java)
        private val EXIF_MAKER = field("photos.exif_maker", String::class.java)
        private val EXIF_MODEL = field("photos.exif_model", String::class.java)
        private val EXIF_APERTURE = field("photos.exif_aperture", String::class.java)
        private val EXIF_SHUTTER = field("photos.exif_shutter", String::class.java)
        private val EXIF_ISO = field("photos.exif_iso", String::class.java)
        private val EXIF_FOCAL_LENGTH = field("photos.exif_focal_length", Long::class.java)
        private val EXIF_LENS = field("photos.exif_lens", String::class.java)
        private val GPS_LATITUDE = field("photos.gps_latitude", BigDecimal::class.java)
        private val GPS_LONGITUDE = field("photos.gps_longitude", BigDecimal::class.java)
        private val CATEGORY_SEQ = field("photos.category_seq", Long::class.java)
        private val CATEGORY_NAME = field("photos_categories.category_name", String::class.java)
        private val TAG_PHOTO_SEQ = field("photos_tags.photo_seq", Long::class.java)
        private val TAG_SEQ = field("photos_tags.tag_seq", Long::class.java)
        private val TAG = field("photos_tags.tag", String::class.java)
    }

    fun findPhotos(photoSearchRequest: PhotoSearchRequest): PageResponse<PhotoSearchResponse> {
        val pageable = PageRequest.of(photoSearchRequest.page, photoSearchRequest.size)
        val baseQuery = dsl.select(PHOTO_SEQ)
            .from(PHOTOS)
            .where(IS_ACTIVE.eq(true))
        if (photoSearchRequest.categorySeq != null) {
            baseQuery.and(CATEGORY_SEQ.eq(photoSearchRequest.categorySeq))
        }

        val photoSeqList = baseQuery
            .orderBy(CREATED_AT.desc())
            .limit(pageable.pageSize)
            .offset((pageable.pageNumber - 1) * pageable.pageSize.toLong())
            .fetch { it.get(PHOTO_SEQ) }
        if (photoSeqList.isEmpty()) {
            return PageResponse(emptyList(), pageable.pageNumber, pageable.pageSize, 0, 0)
        }

        val photoRecords = dsl.select(
            PHOTO_SEQ,
            IMAGE_URL,
            THUMBNAIL_URL,
            TITLE,
            CAPTION,
            CATEGORY_SEQ,
            CATEGORY_NAME,
            LIKE_COUNT,
            SHOT_AT,
            CREATED_AT,
            EXIF_MAKER,
            EXIF_MODEL,
            EXIF_APERTURE,
            EXIF_SHUTTER,
            EXIF_ISO,
            EXIF_FOCAL_LENGTH,
            EXIF_LENS,
            GPS_LATITUDE,
            GPS_LONGITUDE,
            USER_SEQ,
            DISPLAY_NAME,
            PROFILE_IMAGE_URL,
            EMAIL
        )
            .from(PHOTOS)
            .leftJoin(PHOTOS_CATEGORIES).on(CATEGORY_SEQ.eq(field("photos_categories.category_seq", Long::class.java)))
            .leftJoin(USERS).on(USER_SEQ.eq(field("users.user_seq", Long::class.java)))
            .where(PHOTO_SEQ.`in`(photoSeqList))
            .fetch()
        val photoMap = photoRecords.associateBy { it.get(PHOTO_SEQ) }
        val orderedPhotoRecords = photoSeqList.mapNotNull { photoMap[it] }

        val tagMap = dsl.select(TAG_PHOTO_SEQ, TAG)
            .from(PHOTOS_TAGS)
            .where(TAG_PHOTO_SEQ.`in`(photoSeqList))
            .orderBy(TAG_SEQ.asc())
            .fetch()
            .groupBy(
                { it.get(TAG_PHOTO_SEQ) },
                { it.get(TAG) }
            )

        val photos = orderedPhotoRecords.map { record ->
            val photoSeq = record.get(PHOTO_SEQ)
            val tags = tagMap[photoSeq] ?: emptyList()
            val exif = ExifInfo(
                maker = record.get(EXIF_MAKER),
                model = record.get(EXIF_MODEL),
                aperture = record.get(EXIF_APERTURE),
                shutter = record.get(EXIF_SHUTTER),
                iso = record.get(EXIF_ISO),
                focal = record.get(EXIF_FOCAL_LENGTH),
                lens = record.get(EXIF_LENS),
                latitude = record.get(GPS_LATITUDE),
                longitude = record.get(GPS_LONGITUDE)
            )
            PhotoSearchResponse(
                photoSeq = photoSeq,
                src = record.get(IMAGE_URL),
                thumb = record.get(THUMBNAIL_URL),
                title = record.get(TITLE),
                caption = record.get(CAPTION),
                categorySeq = record.get(CATEGORY_SEQ),
                categoryName = record.get(CATEGORY_NAME),
                tags = tags,
                likes = record.get(LIKE_COUNT),
                shotAt = record.get(SHOT_AT),
                createdAt = record.get(CREATED_AT),
                exif = exif,
                userSeq = record.get(USER_SEQ),
                photographerName = record.get(DISPLAY_NAME),
                photographerProfileImage = record.get(PROFILE_IMAGE_URL),
                photographerEmail = record.get(EMAIL)
            )
        }

        val totalCountQuery = dsl.selectCount()
            .from(PHOTOS)
            .where(IS_ACTIVE.eq(true))
        val totalCount = if (photoSearchRequest.categorySeq != null) {
            totalCountQuery.and(CATEGORY_SEQ.eq(photoSearchRequest.categorySeq)).fetchOne(0, Long::class.java) ?: 0L
        } else {
            totalCountQuery.fetchOne(0, Long::class.java) ?: 0L
        }
        val totalPages = if (totalCount == 0L) 0 else ((totalCount - 1) / pageable.pageSize + 1).toInt()
        return PageResponse(photos, pageable.pageNumber, pageable.pageSize, totalCount, totalPages)
    }
}
