package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.dto.PostCategory
import com.walter.lifelog.blog.entity.Category
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * categories 테이블 접근. jOOQ 로 직접 매핑합니다.
 */
@Repository
class CategoriesRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val CATEGORIES = DSL.table("categories")

        private val CATEGORY_SEQ = DSL.field("category_seq", Long::class.java)
        private val CATEGORY_NAME = DSL.field("category_name", String::class.java)
        private val SLUG = DSL.field("slug", String::class.java)
        private val DESCRIPTION = DSL.field("description", String::class.java)
        private val PARENT_CATEGORY_ID = DSL.field("parent_category_id", Long::class.java)
        private val DISPLAY_ORDER = DSL.field("display_order", Int::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        private val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)
        private val IS_ACTIVE = DSL.field("is_active", Boolean::class.java)

        private val ALL_FIELDS = arrayOf(
            CATEGORY_SEQ, CATEGORY_NAME, SLUG, DESCRIPTION, PARENT_CATEGORY_ID,
            DISPLAY_ORDER, CREATED_AT, UPDATED_AT, IS_ACTIVE,
        )

        /** categories 컬럼을 가진 레코드를 [Category] 로 만듭니다. */
        private fun toCategory(record: Record): Category = Category(
            categorySeq = record.get(CATEGORY_SEQ),
            categoryName = record.get(CATEGORY_NAME) ?: "",
            slug = record.get(SLUG) ?: "",
            description = record.get(DESCRIPTION),
            parentCategoryId = record.get(PARENT_CATEGORY_ID),
            displayOrder = record.get(DISPLAY_ORDER) ?: 0,
            createdAt = record.get(CREATED_AT),
            updatedAt = record.get(UPDATED_AT),
            isActive = record.get(IS_ACTIVE) ?: true,
        )
    }

    /**
     * 글 입력 화면의 카테고리 선택 목록입니다.
     *
     * 필요한 것은 시퀀스와 이름뿐이라 두 컬럼만 읽습니다. 예전에는 Category 엔티티
     * 전체를 만들어(description 은 TEXT 입니다) 매퍼로 두 필드만 옮겼습니다.
     * isChecked 는 화면 상태이므로 항상 false 로 시작합니다.
     */
    @Transactional(readOnly = true)
    fun findActivePostCategories(): List<PostCategory> {
        return dsl.select(CATEGORY_SEQ, CATEGORY_NAME)
            .from(CATEGORIES)
            .where(IS_ACTIVE.isTrue)
            .orderBy(CATEGORY_SEQ.asc())
            .fetch()
            .map {
                PostCategory(
                    categorySeq = it.get(CATEGORY_SEQ)!!,
                    categoryName = it.get(CATEGORY_NAME) ?: "",
                )
            }
    }

    /**
     * 활성 카테고리 전체를 category_seq 순으로 돌려줍니다.
     *
     * 이름이 findInActive 인 것은 "비활성" 이 아니라 "활성인 것을(in active) 찾는다" 는
     * 뜻입니다. 기존 JPA 리포지토리의 메서드명을 그대로 유지했습니다.
     */
    @Transactional(readOnly = true)
    fun findInActive(): List<Category> {
        return dsl.select(*ALL_FIELDS)
            .from(CATEGORIES)
            .where(IS_ACTIVE.isTrue)
            .orderBy(CATEGORY_SEQ.asc())
            .fetch()
            .map { toCategory(it) }
    }
}
