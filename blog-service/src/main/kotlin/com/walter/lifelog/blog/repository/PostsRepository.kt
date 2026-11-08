package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.dto.PostSimpleInfo
import com.walter.lifelog.blog.entity.Category
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.code.PostStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SortField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * posts 테이블 접근. jOOQ 로 직접 매핑합니다.
 *
 * 단건 조회는 categories 를 left join 해 [Post.category] 를 채웁니다.
 * 예전에는 `@EntityGraph(attributePaths = ["category"])` 가 하던 일입니다.
 *
 * posts 에는 is_active 컬럼이 없습니다. 논리 삭제는 status = ARCHIVED 로 표현합니다.
 *
 * 필드를 테이블 접두사 없이 선언한 것은 INSERT 의 컬럼 목록과 UPDATE 의 SET 절에
 * 접두사가 붙으면 문법 오류이기 때문입니다. 조인 때문에 이름이 겹치는 카테고리
 * 컬럼만 별칭을 붙여 구분합니다.
 */
@Repository
class PostsRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val POSTS = DSL.table("posts")
        private val CATEGORIES = DSL.table("categories")

        private val POST_SEQ = DSL.field("post_seq", Long::class.java)
        private val USER_SEQ = DSL.field("user_seq", Long::class.java)
        private val CATEGORY_SEQ = DSL.field("category_seq", Long::class.java)
        private val TITLE = DSL.field("title", String::class.java)
        private val SLUG = DSL.field("slug", String::class.java)
        private val SUMMARY = DSL.field("summary", String::class.java)
        private val CONTENT = DSL.field("content", String::class.java)
        private val MARKDOWN_CONTENT = DSL.field("markdown_content", String::class.java)
        private val THUMBNAIL_URL = DSL.field("thumbnail_url", String::class.java)
        private val STATUS = DSL.field("status", String::class.java)
        private val VIEW_COUNT = DSL.field("view_count", Int::class.java)
        private val PUBLISHED_AT = DSL.field("published_at", LocalDateTime::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        private val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)

        /**
         * 조인 조건과 WHERE 절에 쓰는 수식 필드입니다.
         *
         * slug 는 posts 와 categories 양쪽에 있습니다. 조인 쿼리의 WHERE 에
         * 비수식으로 넣으면 MySQL 이 "Column 'slug' in where clause is ambiguous"
         * 로 거부합니다 (H2 는 그냥 첫 테이블로 해석해 넘어갑니다).
         */
        private val POSTS_CATEGORY_SEQ = DSL.field("posts.category_seq", Long::class.java)
        private val CATEGORIES_CATEGORY_SEQ = DSL.field("categories.category_seq", Long::class.java)
        private val POSTS_SLUG = DSL.field("posts.slug", String::class.java)

        // ---------------------------------------------------------------------
        // 조인 SELECT 전용 필드
        //
        // posts 와 categories 에 같은 이름의 컬럼이 넷 있습니다
        // (category_seq / slug / created_at / updated_at). 비수식으로 쓰면 SQL
        // 자체가 모호해지므로 양쪽 모두 별칭을 붙여 구분합니다. 이름이 겹치지 않는
        // 컬럼은 위의 비수식 필드를 그대로 씁니다.
        // ---------------------------------------------------------------------
        private val P_CATEGORY_SEQ: Field<Long> = POSTS_CATEGORY_SEQ.`as`("p_category_seq")
        private val P_SLUG: Field<String> = DSL.field("posts.slug", String::class.java).`as`("p_slug")
        private val P_CREATED_AT: Field<LocalDateTime> = DSL.field("posts.created_at", LocalDateTime::class.java).`as`("p_created_at")
        private val P_UPDATED_AT: Field<LocalDateTime> = DSL.field("posts.updated_at", LocalDateTime::class.java).`as`("p_updated_at")

        private val C_SEQ: Field<Long> = CATEGORIES_CATEGORY_SEQ.`as`("c_category_seq")
        private val C_NAME: Field<String> = DSL.field("categories.category_name", String::class.java).`as`("c_category_name")
        private val C_SLUG: Field<String> = DSL.field("categories.slug", String::class.java).`as`("c_slug")
        private val C_DESCRIPTION: Field<String> = DSL.field("categories.description", String::class.java).`as`("c_description")
        private val C_PARENT_ID: Field<Long> = DSL.field("categories.parent_category_id", Long::class.java).`as`("c_parent_category_id")
        private val C_DISPLAY_ORDER: Field<Int> = DSL.field("categories.display_order", Int::class.java).`as`("c_display_order")
        private val C_CREATED_AT: Field<LocalDateTime> = DSL.field("categories.created_at", LocalDateTime::class.java).`as`("c_created_at")
        private val C_UPDATED_AT: Field<LocalDateTime> = DSL.field("categories.updated_at", LocalDateTime::class.java).`as`("c_updated_at")
        private val C_IS_ACTIVE: Field<Boolean> = DSL.field("categories.is_active", Boolean::class.java).`as`("c_is_active")

        private val JOIN_FIELDS = arrayOf(
            POST_SEQ, USER_SEQ, TITLE, SUMMARY, CONTENT, MARKDOWN_CONTENT,
            THUMBNAIL_URL, STATUS, VIEW_COUNT, PUBLISHED_AT,
            P_CATEGORY_SEQ, P_SLUG, P_CREATED_AT, P_UPDATED_AT,
            C_SEQ, C_NAME, C_SLUG, C_DESCRIPTION, C_PARENT_ID,
            C_DISPLAY_ORDER, C_CREATED_AT, C_UPDATED_AT, C_IS_ACTIVE,
        )
    }

    @Transactional(readOnly = true)
    fun findBySlug(slug: String): Post? = findOneWithCategory(POSTS_SLUG.eq(slug))

    @Transactional(readOnly = true)
    fun findByPostSeq(postSeq: Long): Post? = findOneWithCategory(POST_SEQ.eq(postSeq))

    private fun findOneWithCategory(condition: Condition): Post? {
        return dsl.select(*JOIN_FIELDS)
            .from(POSTS)
            .leftJoin(CATEGORIES).on(POSTS_CATEGORY_SEQ.eq(CATEGORIES_CATEGORY_SEQ))
            .where(condition)
            .limit(1)
            .fetchOne()
            ?.let { toPost(it).copy(category = toCategory(it)) }
    }

    /**
     * 같은 카테고리에서 publishedAt 직전의 발행글입니다.
     *
     * 화면에는 제목과 링크만 쓰이므로 post_seq 와 title 만 읽습니다. 예전에는 행
     * 전체를 엔티티로 만들어 매퍼로 두 필드만 꺼냈는데, posts 에는 content 와
     * markdown_content 가 LONGTEXT 로 들어 있어 본문 두 벌을 읽고 버리는 셈이었습니다.
     */
    @Transactional(readOnly = true)
    fun findPrevPostInfo(categorySeq: Long, publishedAt: LocalDateTime?): PostSimpleInfo? {
        return findNeighbourInfo(categorySeq, PUBLISHED_AT.lt(publishedAt), PUBLISHED_AT.desc())
    }

    /** 같은 카테고리에서 publishedAt 직후의 발행글입니다. */
    @Transactional(readOnly = true)
    fun findNextPostInfo(categorySeq: Long, publishedAt: LocalDateTime?): PostSimpleInfo? {
        return findNeighbourInfo(categorySeq, PUBLISHED_AT.gt(publishedAt), PUBLISHED_AT.asc())
    }

    private fun findNeighbourInfo(
        categorySeq: Long,
        boundary: Condition,
        order: SortField<LocalDateTime>,
    ): PostSimpleInfo? {
        return dsl.select(POST_SEQ, TITLE)
            .from(POSTS)
            .where(
                CATEGORY_SEQ.eq(categorySeq),
                STATUS.eq(PostStatus.PUBLISHED.name),
                boundary,
            )
            .orderBy(order)
            .limit(1)
            .fetchOne()
            ?.let {
                PostSimpleInfo(
                    postSeq = it.get(POST_SEQ)!!,
                    title = it.get(TITLE) ?: "",
                )
            }
    }

    /**
     * 조회수만 읽습니다.
     *
     * 수정 저장 시 기존 조회수를 보존하려고 쓰는데, 예전에는 category 를 조인한
     * 단건 조회로 행 전체를 읽어 이 한 값을 꺼냈습니다.
     */
    @Transactional(readOnly = true)
    fun findViewCount(postSeq: Long): Int? {
        return dsl.select(VIEW_COUNT)
            .from(POSTS)
            .where(POST_SEQ.eq(postSeq))
            .fetchOne(VIEW_COUNT)
    }

    /**
     * 논리 삭제입니다. 물리 삭제는 쓰지 않습니다.
     * 갱신된 행 수를 돌려줍니다.
     */
    @Transactional
    fun archivePost(postSeq: Long, updatedAt: LocalDateTime): Int {
        return dsl.update(POSTS)
            .set(STATUS, PostStatus.ARCHIVED.name)
            .set(UPDATED_AT, updatedAt)
            .where(POST_SEQ.eq(postSeq))
            .execute()
    }

    /**
     * postSeq 가 없으면 insert, 있으면 update 합니다. JPA 의 save 와 같은 자리입니다.
     *
     * created_at / updated_at 은 Hibernate 의 @CreationTimestamp /
     * @UpdateTimestamp 가 하던 일을 여기서 대신합니다. created_at 은 update 시
     * 건드리지 않습니다 (예전 updatable = false).
     *
     * 돌려주는 Post 는 생성된 키와 타임스탬프가 채워진 것입니다. category 는
     * 채우지 않습니다 — 호출부가 저장 직후 category 를 읽지 않습니다.
     */
    @Transactional
    fun save(post: Post): Post {
        val now = LocalDateTime.now()
        return if (post.postSeq == null) insert(post, now) else update(post, now)
    }

    private fun insert(post: Post, now: LocalDateTime): Post {
        return dsl.insertInto(POSTS)
            .set(USER_SEQ, post.userSeq)
            .set(CATEGORY_SEQ, post.categorySeq)
            .set(TITLE, post.title)
            .set(SLUG, post.slug)
            .set(SUMMARY, post.summary)
            .set(CONTENT, post.content)
            .set(MARKDOWN_CONTENT, post.markdownContent)
            .set(THUMBNAIL_URL, post.thumbnailUrl)
            .set(STATUS, post.status.name)
            .set(VIEW_COUNT, post.viewCount)
            .set(PUBLISHED_AT, post.publishedAt)
            .set(CREATED_AT, now)
            .set(UPDATED_AT, now)
            .returning(POST_SEQ)
            .fetchOne()
            ?.get(POST_SEQ)
            .let { generatedSeq ->
                post.copy(postSeq = generatedSeq, createdAt = now, updatedAt = now)
            }
    }

    private fun update(post: Post, now: LocalDateTime): Post {
        dsl.update(POSTS)
            .set(USER_SEQ, post.userSeq)
            .set(CATEGORY_SEQ, post.categorySeq)
            .set(TITLE, post.title)
            .set(SLUG, post.slug)
            .set(SUMMARY, post.summary)
            .set(CONTENT, post.content)
            .set(MARKDOWN_CONTENT, post.markdownContent)
            .set(THUMBNAIL_URL, post.thumbnailUrl)
            .set(STATUS, post.status.name)
            .set(VIEW_COUNT, post.viewCount)
            .set(PUBLISHED_AT, post.publishedAt)
            .set(UPDATED_AT, now)
            .where(POST_SEQ.eq(post.postSeq))
            .execute()

        return post.copy(updatedAt = now)
    }

    /** [JOIN_FIELDS] 로 조회한 레코드를 읽습니다. */
    private fun toPost(record: Record): Post = Post(
        postSeq = record.get(POST_SEQ),
        userSeq = record.get(USER_SEQ) ?: 0L,
        categorySeq = record.get(P_CATEGORY_SEQ),
        category = null,
        title = record.get(TITLE) ?: "",
        slug = record.get(P_SLUG),
        summary = record.get(SUMMARY),
        content = record.get(CONTENT) ?: "",
        markdownContent = record.get(MARKDOWN_CONTENT),
        thumbnailUrl = record.get(THUMBNAIL_URL),
        status = record.get(STATUS)?.let { PostStatus.valueOf(it) } ?: PostStatus.DRAFT,
        viewCount = record.get(VIEW_COUNT) ?: 0,
        publishedAt = record.get(PUBLISHED_AT),
        createdAt = record.get(P_CREATED_AT),
        updatedAt = record.get(P_UPDATED_AT),
    )

    /** left join 결과입니다. 카테고리가 없는 글이면 null 입니다. */
    private fun toCategory(record: Record): Category? {
        val categorySeq = record.get(C_SEQ) ?: return null
        return Category(
            categorySeq = categorySeq,
            categoryName = record.get(C_NAME) ?: "",
            slug = record.get(C_SLUG) ?: "",
            description = record.get(C_DESCRIPTION),
            parentCategoryId = record.get(C_PARENT_ID),
            displayOrder = record.get(C_DISPLAY_ORDER) ?: 0,
            createdAt = record.get(C_CREATED_AT),
            updatedAt = record.get(C_UPDATED_AT),
            isActive = record.get(C_IS_ACTIVE) ?: true,
        )
    }
}
