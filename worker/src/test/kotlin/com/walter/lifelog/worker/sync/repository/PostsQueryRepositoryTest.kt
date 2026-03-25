package com.walter.lifelog.worker.sync.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations

@DisplayName("PostsQueryRepository 테스트")
class PostsQueryRepositoryTest {
    private val dsl: DSLContext = mockk(relaxed = true)
    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val repository = PostsQueryRepository(dsl, redisTemplate)

    @BeforeEach
    fun setUp() {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    @DisplayName("findPublishedPostSequences - PUBLISHED 상태의 게시글 시퀀스 목록을 반환한다")
    fun findPublishedPostSequences_returnsPublishedPostSeqs() {
        // given - relaxed mock으로 jOOQ 체이닝 자동 처리

        // when
        val actual = repository.findPublishedPostSequences()

        // then
        assertEquals(emptyList<Long>(), actual)
        verify { dsl.select(any<org.jooq.Field<Long>>()) }
    }

    @Test
    @DisplayName("updateViewCount - Redis에 조회수가 있으면 DB 업데이트를 수행한다")
    fun updateViewCount_updatesDbWhenRedisHasValue() {
        // given
        every { valueOps.get("post_1") } returns "150"

        // when
        repository.updateViewCount(1L)

        // then
        verify { valueOps.get("post_1") }
        verify { dsl.update(any<org.jooq.Table<*>>()) }
    }

    @Test
    @DisplayName("updateViewCount - Redis에 조회수가 없으면 0을 반환하고 DB를 업데이트하지 않는다")
    fun updateViewCount_returnsZeroWhenRedisHasNoValue() {
        // given
        every { valueOps.get("post_2") } returns null

        // when
        val result = repository.updateViewCount(2L)

        // then
        assertEquals(0, result)
        verify { valueOps.get("post_2") }
        verify(exactly = 0) { dsl.update(any<org.jooq.Table<*>>()) }
    }

    @Test
    @DisplayName("updateViewCount - Redis 값이 숫자가 아니면 0을 반환한다")
    fun updateViewCount_returnsZeroWhenRedisValueIsNotNumeric() {
        // given
        every { valueOps.get("post_3") } returns "not_a_number"

        // when
        val result = repository.updateViewCount(3L)

        // then
        assertEquals(0, result)
        verify { valueOps.get("post_3") }
        verify(exactly = 0) { dsl.update(any<org.jooq.Table<*>>()) }
    }
}
