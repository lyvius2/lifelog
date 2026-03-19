package com.walter.lifelog.blog.repository

import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PostsQueryRepository(
    private val dsl: DSLContext,
    private val virtualThreadExecutor: TaskExecutor,
) {
    companion object {
        private val POSTS = DSL.table("posts")
        private val CATEGORIES = DSL.table("categories")
        private val USERS = DSL.table("users")

        private val POST_SEQ = DSL.field("posts.post_seq", Long::class.java)
        private val TITLE = DSL.field("posts.title", String::class.java)
        private val SUMMARY = DSL.field("posts.summary", String::class.java)
        private val THUMBNAIL_URL = DSL.field("posts.thumbnail_url", String::class.java)
        private val STATUS = DSL.field("posts.status", String::class.java)
        private val VIEW_COUNT = DSL.field("posts.view_count", Int::class.java)
        private val PUBLISHED_AT = DSL.field("posts.published_at", LocalDateTime::class.java)
        private val CREATED_AT = DSL.field("posts.created_at", LocalDateTime::class.java)
        private val CATEGORY_SEQ = DSL.field("posts.category_seq", Long::class.java)
        private val CATEGORY_NAME = DSL.field("categories.category_name", String::class.java)
        private val USER_SEQ = DSL.field("posts.user_seq", Long::class.java)
        private val DISPLAY_NAME = DSL.field("users.display_name", String::class.java)
        private val PROFILE_IMAGE_URL = DSL.field("users.profile_image_url", String::class.java)
    }

    fun findSearchedPosts(postSearchCondition: PostSearchCondition): PageResponse<PostListResponse> {
        val conditions = buildConditions(postSearchCondition)

        val totalCountFuture = asyncSupply(virtualThreadExecutor) {
             dsl.selectCount()
                .from(POSTS)
                .leftJoin(CATEGORIES).on(CATEGORY_SEQ.eq(DSL.field("categories.category_seq", Long::class.java)))
                .leftJoin(USERS).on(USER_SEQ.eq(DSL.field("users.user_seq", Long::class.java)))
                .where(conditions)
                .fetchOne(0, Long::class.java) ?: 0L
        }

        val recordsFuture = asyncSupply(virtualThreadExecutor) {
            dsl.select(
                POST_SEQ,
                TITLE,
                SUMMARY,
                THUMBNAIL_URL,
                CATEGORY_NAME,
                STATUS,
                VIEW_COUNT,
                PUBLISHED_AT,
                CREATED_AT,
                DISPLAY_NAME,
                PROFILE_IMAGE_URL
            )
                .from(POSTS)
                .leftJoin(CATEGORIES).on(CATEGORY_SEQ.eq(DSL.field("categories.category_seq", Long::class.java)))
                .leftJoin(USERS).on(USER_SEQ.eq(DSL.field("users.user_seq", Long::class.java)))
                .where(conditions)
                .orderBy(
                    PUBLISHED_AT.sort(SortOrder.DESC).nullsLast(),
                    CREATED_AT.desc(),
                )
                .limit(postSearchCondition.size)
                .offset((postSearchCondition.page - 1) * postSearchCondition.size)
                .fetch()
        }

        val records = recordsFuture.get()
        val totalCount = totalCountFuture.get()
        val totalPages = if (totalCount == 0L) 0 else ((totalCount - 1) / postSearchCondition.size + 1).toInt()
        if (totalCount == 0L) {
            return PageResponse(emptyList(), postSearchCondition.page, postSearchCondition.size, 0, 0)
        }

        val postSeqs = records.map { it.get(POST_SEQ)!! }
        val tagsMap = fetchTagsMap(postSeqs)

        val content = records.map { record ->
            val postSeq = record.get(POST_SEQ)!!
            PostListResponse(
                postSeq = postSeq,
                title = record.get(TITLE)!!,
                summary = record.get(SUMMARY),
                thumbnailUrl = record.get(THUMBNAIL_URL),
                categoryName = record.get(CATEGORY_NAME),
                status = record.get(STATUS)!!,
                viewCount = record.get(VIEW_COUNT) ?: 0,
                tags = tagsMap[postSeq] ?: emptyList(),
                publishedAt = record.get(PUBLISHED_AT),
                createdAt = record.get(CREATED_AT),
                writerName = record.get(DISPLAY_NAME),
                writerProfileImage = record.get(PROFILE_IMAGE_URL),
            )
        }

        return PageResponse(content, postSearchCondition.page, postSearchCondition.size, totalCount, totalPages)
    }

    private fun buildConditions(postSearchCondition: PostSearchCondition): List<Condition> {
        val conditions = mutableListOf<Condition>()

        postSearchCondition.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            val pattern = "%$keyword%"
            conditions.add(
                TITLE.likeIgnoreCase(pattern).or(SUMMARY.likeIgnoreCase(pattern))
            )
        }

        postSearchCondition.categorySeq?.let { catSeq ->
            val categorySeqField = DSL.field("category_seq", Long::class.java)
            val cteName = DSL.name("category_tree")
            val cteTable = DSL.table(cteName).`as`("ct")
            val recursiveCte = cteName.`as`(
                DSL.select(categorySeqField)
                    .from(DSL.table("categories"))
                    .where(categorySeqField.eq(catSeq))
                    .unionAll(
                        DSL.select(DSL.field("c.category_seq", Long::class.java))
                            .from(DSL.table("categories").`as`("c"))
                            .innerJoin(cteTable).on(DSL.field("c.parent_category_id", Long::class.java).eq(DSL.field("ct.category_seq", Long::class.java)))
                    )
            )
            val subquery = dsl.withRecursive(recursiveCte)
                .select(DSL.field("ct.category_seq", Long::class.java))
                .from(cteTable)
            conditions.add(CATEGORY_SEQ.`in`(subquery))
        }

        val status = postSearchCondition.status?.takeIf { it.isNotBlank() } ?: PostStatus.PUBLISHED.name
        conditions.add(STATUS.eq(status))

        postSearchCondition.tag?.takeIf { it.isNotBlank() }?.let { tag ->
            val subquery = DSL.select(DSL.field("posts_tags.post_seq", Long::class.java))
                .from(DSL.table("posts_tags"))
                .where(DSL.field("posts_tags.tag", String::class.java).eq(tag))
            conditions.add(POST_SEQ.`in`(subquery))
        }

        return conditions
    }

    private fun fetchTagsMap(postSeqs: List<Long>): Map<Long, List<String>> {
        if (postSeqs.isEmpty()) return emptyMap()

        return dsl.select(
            DSL.field("posts_tags.post_seq", Long::class.java),
            DSL.field("posts_tags.tag", String::class.java),
        )
            .from(DSL.table("posts_tags"))
            .where(DSL.field("posts_tags.post_seq", Long::class.java).`in`(postSeqs))
            .orderBy(DSL.field("posts_tags.tag_seq", Int::class.java).asc())
            .fetch()
            .groupBy(
                { it.get(DSL.field("posts_tags.post_seq", Long::class.java))!! },
                { it.get(DSL.field("posts_tags.tag", String::class.java))!! },
            )
    }
}
