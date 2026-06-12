package com.walter.lifelog.user.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.walter.lifelog.user.config.SessionProperties
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@DisplayName("SessionService 테스트")
class SessionServiceTest {

    private val redisTemplate: StringRedisTemplate = mockk()
    private val objectMapper = jacksonObjectMapper()
    private val sessionProperties = SessionProperties(
        keyPrefix = "spring:session:sessions:",
        attrField = "sessionAttr:loginMember",
        ttl = Duration.ofSeconds(1800),
    )
    private val sessionService = SessionService(redisTemplate, objectMapper, sessionProperties)

    @Test
    @DisplayName("saveAdminSession - Redis Hash에 관리자 세션 정보를 저장하고 TTL을 1800초로 설정한다")
    fun saveAdminSession_shouldSaveHashAndSetTtlOf1800Seconds() {
        // given
        val hashOps: HashOperations<String, String, String> = mockk()
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.put(any(), any(), any()) } just Runs
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true

        val sessionId = "test-session-id"
        val expectedKey = "spring:session:sessions:$sessionId"

        // when
        sessionService.saveAdminSession(sessionId, 1L, "admin@example.com", "Walter")

        // then
        verify(exactly = 1) { hashOps.put(expectedKey, "sessionAttr:loginMember", any()) }
        verify(exactly = 1) { redisTemplate.expire(expectedKey, Duration.ofSeconds(1800)) }
    }

    @Test
    @DisplayName("saveAdminSession - 저장되는 JSON에 userId, email, username, isAdmin 필드가 포함된다")
    fun saveAdminSession_shouldStoreJsonWithAllRequiredFields() {
        // given
        val hashOps: HashOperations<String, String, String> = mockk()
        val jsonSlot = slot<String>()
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.put(any(), any(), capture(jsonSlot)) } just Runs
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true

        // when
        sessionService.saveAdminSession("test-session-id", 42L, "admin@example.com", "Walter")

        // then
        val json = jsonSlot.captured
        assertThat(json).contains("\"userId\":\"42\"")
        assertThat(json).contains("\"email\":\"admin@example.com\"")
        assertThat(json).contains("\"username\":\"Walter\"")
        assertThat(json).contains("\"isAdmin\":true")
    }

    @Test
    @DisplayName("extendSessionTtl - 해당 세션 키의 TTL을 1800초로 연장한다")
    fun extendSessionTtl_shouldResetTtlOf1800Seconds() {
        // given
        val sessionId = "test-session-id"
        val expectedKey = "spring:session:sessions:$sessionId"
        every { redisTemplate.expire(expectedKey, Duration.ofSeconds(1800)) } returns true

        // when
        sessionService.extendSessionTtl(sessionId)

        // then
        verify(exactly = 1) { redisTemplate.expire(expectedKey, Duration.ofSeconds(1800)) }
    }
}