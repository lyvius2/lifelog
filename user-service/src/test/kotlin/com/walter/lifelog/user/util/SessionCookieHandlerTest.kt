package com.walter.lifelog.user.util

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

@DisplayName("CookieHandler 테스트")
class SessionCookieHandlerTest {

    private val sessionCookieHandler = SessionCookieHandler()

    @Test
    @DisplayName("addSessionCookie - LIFELOG_SESSION 쿠키를 HttpOnly, SameSite=Lax, Max-Age=1800으로 응답에 추가한다")
    fun addSessionCookie_shouldAddCookieHeaderWithCorrectAttributes() {
        // given
        val response: HttpServletResponse = mockk()
        val headerSlot = slot<String>()
        every { response.addHeader(HttpHeaders.SET_COOKIE, capture(headerSlot)) } just Runs

        // when
        sessionCookieHandler.addSessionCookie(response, "test-session-id")

        // then
        verify(exactly = 1) { response.addHeader(HttpHeaders.SET_COOKIE, any()) }
        assertThat(headerSlot.captured).contains("${SessionCookieHandler.COOKIE_NAME}=test-session-id")
        assertThat(headerSlot.captured).contains("Max-Age=1800")
        assertThat(headerSlot.captured).contains("Path=/")
        assertThat(headerSlot.captured).contains("HttpOnly")
        assertThat(headerSlot.captured).contains("SameSite=Lax")
    }

    @Test
    @DisplayName("expireSessionCookie - Max-Age=0으로 LIFELOG_SESSION 쿠키를 즉시 만료시킨다")
    fun expireSessionCookie_shouldAddCookieHeaderWithMaxAgeZero() {
        // given
        val response: HttpServletResponse = mockk()
        val headerSlot = slot<String>()
        every { response.addHeader(HttpHeaders.SET_COOKIE, capture(headerSlot)) } just Runs

        // when
        sessionCookieHandler.expireSessionCookie(response)

        // then
        verify(exactly = 1) { response.addHeader(HttpHeaders.SET_COOKIE, any()) }
        assertThat(headerSlot.captured).contains("SameSite=Lax")
        assertThat(headerSlot.captured).contains("Path=/")
        assertThat(headerSlot.captured).contains("${SessionCookieHandler.COOKIE_NAME}=;")  // 빈 값 확인
        assertThat(headerSlot.captured).contains("Max-Age=0")
        assertThat(headerSlot.captured).contains("HttpOnly")
    }
}