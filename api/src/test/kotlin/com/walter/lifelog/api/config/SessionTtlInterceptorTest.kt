package com.walter.lifelog.api.config

import com.walter.lifelog.user.config.SessionProperties
import com.walter.lifelog.user.util.SessionCookieHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@DisplayName("SessionTtlInterceptor 테스트")
class SessionTtlInterceptorTest {

    private val redisTemplate: StringRedisTemplate = mockk(relaxed = true)
    private val sessionProperties = SessionProperties(
        keyPrefix = "spring:session:sessions:",
        attrField = "sessionAttr:loginMember",
        ttl = Duration.ofSeconds(1800),
    )
    private val interceptor = SessionTtlInterceptor(redisTemplate, sessionProperties)

    @Test
    @DisplayName("preHandle - LIFELOG_SESSION 쿠키가 있으면 Redis TTL을 연장하고 true를 반환한다")
    fun preHandle_shouldExtendTtlAndReturnTrueWhenCookiePresent() {
        // given
        val request: HttpServletRequest = mockk()
        val response: HttpServletResponse = mockk()
        val handler = Any()
        val sessionId = "test-session-id"
        val expectedKey = "spring:session:sessions:$sessionId"

        every { request.cookies } returns arrayOf(Cookie(SessionCookieHandler.COOKIE_NAME, sessionId))
        every { redisTemplate.expire(expectedKey, Duration.ofSeconds(1800)) } returns true

        // when
        val result = interceptor.preHandle(request, response, handler)

        // then
        assertThat(result).isTrue()
        verify(exactly = 1) { redisTemplate.expire(expectedKey, Duration.ofSeconds(1800)) }
    }

    @Test
    @DisplayName("preHandle - LIFELOG_SESSION 쿠키가 없으면 Redis TTL 연장 없이 true를 반환한다")
    fun preHandle_shouldNotExtendTtlAndReturnTrueWhenNoCookie() {
        // given
        val request: HttpServletRequest = mockk()
        val response: HttpServletResponse = mockk()
        val handler = Any()

        every { request.cookies } returns null

        // when
        val result = interceptor.preHandle(request, response, handler)

        // then
        assertThat(result).isTrue()
        verify(exactly = 0) { redisTemplate.expire(any<String>(), any<Duration>()) }
    }

    @Test
    @DisplayName("preHandle - 다른 쿠키만 있으면 Redis TTL 연장 없이 true를 반환한다")
    fun preHandle_shouldNotExtendTtlWhenOnlyOtherCookiesPresent() {
        // given
        val request: HttpServletRequest = mockk()
        val response: HttpServletResponse = mockk()
        val handler = Any()

        every { request.cookies } returns arrayOf(Cookie("OTHER_COOKIE", "some-value"))

        // when
        val result = interceptor.preHandle(request, response, handler)

        // then
        assertThat(result).isTrue()
        verify(exactly = 0) { redisTemplate.expire(any<String>(), any<Duration>()) }
    }
}