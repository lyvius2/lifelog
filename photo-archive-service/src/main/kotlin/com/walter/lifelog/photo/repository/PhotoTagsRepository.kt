package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.entity.PhotoTag
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * photos_tags 테이블 접근. jOOQ 로 직접 매핑합니다.
 */
@Repository
class PhotoTagsRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val PHOTOS_TAGS = DSL.table("photos_tags")

        private val PHOTO_SEQ = DSL.field("photo_seq", Long::class.java)
        private val TAG_SEQ = DSL.field("tag_seq", Int::class.java)
        private val TAG = DSL.field("tag", String::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
    }

    @Transactional(readOnly = true)
    fun findByPhotoSeq(photoSeq: Long): List<PhotoTag> {
        return dsl.select(PHOTO_SEQ, TAG_SEQ, TAG, CREATED_AT)
            .from(PHOTOS_TAGS)
            .where(PHOTO_SEQ.eq(photoSeq))
            .orderBy(TAG_SEQ.asc())
            .fetch()
            .map {
                PhotoTag(
                    photoSeq = it.get(PHOTO_SEQ)!!,
                    tagSeq = it.get(TAG_SEQ) ?: 0,
                    tag = it.get(TAG) ?: "",
                    createdAt = it.get(CREATED_AT),
                )
            }
    }

    @Transactional
    fun deleteByPhotoSeq(photoSeq: Long): Int {
        return dsl.deleteFrom(PHOTOS_TAGS)
            .where(PHOTO_SEQ.eq(photoSeq))
            .execute()
    }

    /** 태그를 한 번의 멀티 insert 로 넣습니다. */
    @Transactional
    fun saveAll(photoTags: List<PhotoTag>): List<PhotoTag> {
        if (photoTags.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        var insert = dsl.insertInto(PHOTOS_TAGS, PHOTO_SEQ, TAG_SEQ, TAG, CREATED_AT)
        photoTags.forEach { insert = insert.values(it.photoSeq, it.tagSeq, it.tag, now) }
        insert.execute()

        return photoTags.map { it.copy(createdAt = now) }
    }
}
