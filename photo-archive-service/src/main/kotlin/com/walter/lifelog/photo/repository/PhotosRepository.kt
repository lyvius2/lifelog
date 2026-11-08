package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.dto.PhotoShotPeriod
import com.walter.lifelog.photo.entity.Photo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * photos 테이블 접근. jOOQ 로 직접 매핑합니다.
 *
 * 수정은 필요한 컬럼만 UPDATE 합니다. 예전에는 행 전체를 읽어 엔티티를 만들고
 * 한 필드를 바꿔 save 로 전체를 다시 쓰는 식이었는데, photos 는 컬럼이 24개라
 * 좋아요 수 하나를 바꾸는 데도 전부 오갔습니다.
 */
@Repository
class PhotosRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val PHOTOS = DSL.table("photos")

        private val PHOTO_SEQ = DSL.field("photo_seq", Long::class.java)
        private val USER_SEQ = DSL.field("user_seq", Long::class.java)
        private val TITLE = DSL.field("title", String::class.java)
        private val CAPTION = DSL.field("caption", String::class.java)
        private val CATEGORY_SEQ = DSL.field("category_seq", Long::class.java)
        private val IMAGE_URL = DSL.field("image_url", String::class.java)
        private val THUMBNAIL_URL = DSL.field("thumbnail_url", String::class.java)
        private val LIKE_COUNT = DSL.field("like_count", Int::class.java)
        private val EXIF_MAKER = DSL.field("exif_maker", String::class.java)
        private val EXIF_MODEL = DSL.field("exif_model", String::class.java)
        private val EXIF_APERTURE = DSL.field("exif_aperture", String::class.java)
        private val EXIF_SHUTTER = DSL.field("exif_shutter", String::class.java)
        private val EXIF_ISO = DSL.field("exif_iso", String::class.java)
        private val EXIF_FOCAL_LENGTH = DSL.field("exif_focal_length", Long::class.java)
        private val EXIF_LENS = DSL.field("exif_lens", String::class.java)
        private val EXIF_FLASH = DSL.field("exif_flash", String::class.java)
        private val GPS_LATITUDE = DSL.field("gps_latitude", BigDecimal::class.java)
        private val GPS_LONGITUDE = DSL.field("gps_longitude", BigDecimal::class.java)
        private val SHOT_AT = DSL.field("shot_at", LocalDateTime::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        private val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)
        private val IS_ACTIVE = DSL.field("is_active", Boolean::class.java)
    }

    /**
     * 사진을 넣고 생성된 photo_seq 를 돌려줍니다.
     *
     * 엔티티 전체를 되돌려 주지 않는 것은 호출부가 시퀀스만 쓰기 때문입니다
     * (태그를 붙일 대상 키).
     */
    @Transactional
    fun insert(photo: Photo): Long {
        val now = LocalDateTime.now()
        return dsl.insertInto(PHOTOS)
            .set(USER_SEQ, photo.userSeq)
            .set(TITLE, photo.title)
            .set(CAPTION, photo.caption)
            .set(CATEGORY_SEQ, photo.categorySeq)
            .set(IMAGE_URL, photo.imageUrl)
            .set(THUMBNAIL_URL, photo.thumbnailUrl)
            .set(LIKE_COUNT, photo.likeCount)
            .set(EXIF_MAKER, photo.exifMaker)
            .set(EXIF_MODEL, photo.exifModel)
            .set(EXIF_APERTURE, photo.exifAperture)
            .set(EXIF_SHUTTER, photo.exifShutter)
            .set(EXIF_ISO, photo.exifIso)
            .set(EXIF_FOCAL_LENGTH, photo.exifFocalLength)
            .set(EXIF_LENS, photo.exifLens)
            .set(EXIF_FLASH, photo.exifFlash)
            .set(GPS_LATITUDE, photo.gpsLatitude)
            .set(GPS_LONGITUDE, photo.gpsLongitude)
            .set(SHOT_AT, photo.shotAt)
            .set(CREATED_AT, now)
            .set(UPDATED_AT, now)
            .set(IS_ACTIVE, photo.isActive)
            .returning(PHOTO_SEQ)
            .fetchOne()
            ?.get(PHOTO_SEQ)
            ?: throw IllegalStateException("사진 저장 후 photo_seq 를 받지 못했습니다")
    }

    /**
     * 논리 삭제·복구에 씁니다. 물리 삭제는 쓰지 않습니다.
     * 갱신된 행 수를 돌려줍니다.
     */
    @Transactional
    fun updateActive(photoSeq: Long, isActive: Boolean): Int {
        return dsl.update(PHOTOS)
            .set(IS_ACTIVE, isActive)
            .set(UPDATED_AT, LocalDateTime.now())
            .where(PHOTO_SEQ.eq(photoSeq))
            .execute()
    }

    /** 갱신된 행 수를 돌려줍니다. */
    @Transactional
    fun updateLikeCount(photoSeq: Long, likeCount: Int): Int {
        return dsl.update(PHOTOS)
            .set(LIKE_COUNT, likeCount)
            .set(UPDATED_AT, LocalDateTime.now())
            .where(PHOTO_SEQ.eq(photoSeq))
            .execute()
    }

    /**
     * 촬영 시점의 최소·최대 연도입니다.
     *
     * 예전에는 가장 이른 사진과 가장 늦은 사진의 **행 전체**를 각각 조회해
     * (가상 스레드로 병렬까지 해서) shotAt.year 만 꺼냈습니다. 집계 한 번으로
     * 끝나는 일이라 쿼리 하나로 합쳤습니다.
     */
    @Transactional(readOnly = true)
    fun findShotPeriod(): PhotoShotPeriod {
        val minShotAt = DSL.min(SHOT_AT)
        val maxShotAt = DSL.max(SHOT_AT)

        val record = dsl.select(minShotAt, maxShotAt)
            .from(PHOTOS)
            .where(SHOT_AT.isNotNull, IS_ACTIVE.isTrue)
            .fetchOne()

        return PhotoShotPeriod(
            minYear = record?.get(minShotAt)?.year,
            maxYear = record?.get(maxShotAt)?.year,
        )
    }
}
