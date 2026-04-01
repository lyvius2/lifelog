package com.walter.lifelog.worker.main.database.repository

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class PhotosQueryRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val TABLE = DSL.table("lifelog.photos")
        private val FIELD_PHOTO_SEQ = DSL.field("lifelog.photos.photo_seq", Long::class.java)
        private val FIELD_STATUS = DSL.field("lifelog.photos.status", String::class.java)
        private val FIELD_THUMBNAIL_URL = DSL.field("lifelog.photos.thumbnail_url", String::class.java)
        private val FIELD_UPDATED_AT = DSL.field("lifelog.photos.updated_at", LocalDateTime::class.java)
    }

    @Transactional
    fun updatePhoto(photoSeq: Long, status: String, thumbnailUrl: String?) {
        dsl.update(TABLE)
            .set(FIELD_STATUS, status)
            .apply {
                if (thumbnailUrl != null) {
                    set(FIELD_THUMBNAIL_URL, thumbnailUrl)
                }
            }
            .set(FIELD_UPDATED_AT, LocalDateTime.now())
            .where(FIELD_PHOTO_SEQ.eq(photoSeq))
            .execute()
    }
}
