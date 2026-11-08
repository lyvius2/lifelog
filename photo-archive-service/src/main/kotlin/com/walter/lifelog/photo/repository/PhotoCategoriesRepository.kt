package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.entity.PhotoCategory
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * photos_categories 테이블 접근. jOOQ 로 직접 매핑합니다.
 */
@Repository
class PhotoCategoriesRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val PHOTOS_CATEGORIES = DSL.table("photos_categories")

        private val CATEGORY_SEQ = DSL.field("category_seq", Long::class.java)
        private val CATEGORY_NAME = DSL.field("category_name", String::class.java)
        private val ICON = DSL.field("icon", String::class.java)
        private val IS_ACTIVE = DSL.field("is_active", Boolean::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        private val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)

        private val ALL_FIELDS = arrayOf(
            CATEGORY_SEQ, CATEGORY_NAME, ICON, IS_ACTIVE, CREATED_AT, UPDATED_AT,
        )

        /** photos_categories 컬럼을 가진 레코드를 [PhotoCategory] 로 만듭니다. */
        private fun toPhotoCategory(record: Record): PhotoCategory = PhotoCategory(
            categorySeq = record.get(CATEGORY_SEQ),
            categoryName = record.get(CATEGORY_NAME) ?: "",
            icon = record.get(ICON),
            isActive = record.get(IS_ACTIVE) ?: true,
            createdAt = record.get(CREATED_AT),
            updatedAt = record.get(UPDATED_AT),
        )
    }

    /**
     * 활성 카테고리의 응답 DTO 입니다.
     *
     * 응답이 쓰는 것은 시퀀스·아이콘·이름 세 개라 그만 읽습니다. 예전에는
     * PhotoCategory 엔티티를 만들어 매퍼로 세 필드를 옮겼습니다.
     */
    @Transactional(readOnly = true)
    fun findActiveCategoryResponses(): List<PhotoCategoryResponse> {
        return dsl.select(CATEGORY_SEQ, ICON, CATEGORY_NAME)
            .from(PHOTOS_CATEGORIES)
            .where(IS_ACTIVE.isTrue)
            .orderBy(CATEGORY_SEQ.asc())
            .fetch()
            .map {
                PhotoCategoryResponse(
                    categorySeq = it.get(CATEGORY_SEQ)!!,
                    icon = it.get(ICON) ?: "",
                    name = it.get(CATEGORY_NAME) ?: "",
                )
            }
    }
}
