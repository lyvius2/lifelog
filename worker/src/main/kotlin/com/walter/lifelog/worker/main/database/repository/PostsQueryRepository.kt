package com.walter.lifelog.worker.main.database.repository

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PostsQueryRepository(
    @Qualifier("mysqlDslContext") private val dsl: DSLContext,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private val POSTS = DSL.table("posts")
        private val POST_SEQ = DSL.field("posts.post_seq", Long::class.java)
        private val STATUS = DSL.field("posts.status", String::class.java)
        private val VIEW_COUNT = DSL.field("posts.view_count", Int::class.java)
    }

    @Transactional(readOnly = true)
    fun findPublishedPostSequences(): List<Long> {
        return dsl.select(POST_SEQ)
            .from(POSTS)
            .where(STATUS.eq("PUBLISHED"))
            .fetch(POST_SEQ)
    }

    @Transactional
    fun updateViewCount(postSeq: Long): Int {
        val key = "post_$postSeq"
        val viewCount = redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: return 0
        return dsl.update(POSTS)
            .set(VIEW_COUNT, viewCount)
            .where(POST_SEQ.eq(postSeq))
            .execute()
    }
}

