package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.entity.PostTag
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * posts_tags 테이블 접근. jOOQ 로 직접 매핑합니다.
 */
@Repository
class PostTagsRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val POSTS_TAGS = DSL.table("posts_tags")

        private val POST_SEQ = DSL.field("post_seq", Long::class.java)
        private val TAG_SEQ = DSL.field("tag_seq", Int::class.java)
        private val TAG = DSL.field("tag", String::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
    }

    /**
     * 태그 문자열만 tag_seq 순으로 돌려줍니다.
     *
     * 호출부가 쓰는 것은 문자열 목록뿐입니다. 예전에는 PostTag 엔티티를 줄줄이
     * 만들어 거기서 tag 만 뽑아 썼습니다.
     */
    @Transactional(readOnly = true)
    fun findTagsByPostSeq(postSeq: Long): List<String> {
        return dsl.select(TAG)
            .from(POSTS_TAGS)
            .where(POST_SEQ.eq(postSeq))
            .orderBy(TAG_SEQ.asc())
            .fetch(TAG)
    }

    @Transactional
    fun deleteByPostSeq(postSeq: Long): Int {
        return dsl.deleteFrom(POSTS_TAGS)
            .where(POST_SEQ.eq(postSeq))
            .execute()
    }

    /**
     * 태그를 한 번의 멀티 insert 로 넣습니다. created_at 은 Hibernate 의
     * @CreationTimestamp 가 하던 일을 여기서 대신합니다.
     */
    @Transactional
    fun saveAll(postTags: List<PostTag>): List<PostTag> {
        if (postTags.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        var insert = dsl.insertInto(POSTS_TAGS, POST_SEQ, TAG_SEQ, TAG, CREATED_AT)
        postTags.forEach { insert = insert.values(it.postSeq, it.tagSeq, it.tag, now) }
        insert.execute()

        return postTags.map { it.copy(createdAt = now) }
    }
}
