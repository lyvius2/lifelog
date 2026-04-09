package com.walter.lifelog.api.config.aop

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.shared.util.TokenHandler
import io.mockk.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.aspectj.lang.ProceedingJoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class AdminRequiredAspectTest {
    private val jwtSecretKey = "testSecretKey"
    private val aspect = AdminRequiredAspect(jwtSecretKey)
    private val joinPoint: ProceedingJoinPoint = mockk()
    private val adminRequired: AdminRequired = mockk()

    @BeforeEach
    fun setUp() {
        every { joinPoint.proceed() } returns "OK"
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    @DisplayName("JWT 토큰이 유효하면 userSeq를 request attribute에 저장하고 메서드를 실행한다")
    fun validJwtToken_shouldSetUserSeqAndProceed() {
        // given
        val request = MockHttpServletRequest()
        val token = TokenHandler.generateAccessToken("test@test.com", 1L, "Walter", jwtSecretKey)
        request.addHeader("Authorization", "Bearer $token")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        // when
        val result = aspect.around(joinPoint, adminRequired)

        // then
        assertThat(result).isEqualTo("OK")
        assertThat(request.getAttribute("userSeq")).isEqualTo(1L)
        verify { joinPoint.proceed() }
    }

    @Test
    @DisplayName("JWT 토큰이 유효하지 않으면 LoginException을 던진다")
    fun invalidJwtToken_shouldThrowException() {
        // given
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalid.token.value")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        // when & then
        assertThatThrownBy { aspect.around(joinPoint, adminRequired) }
            .isInstanceOf(LoginException::class.java)
        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    @DisplayName("세션에 userSeq가 있으면 request attribute에 저장하고 메서드를 실행한다")
    fun validSession_shouldSetUserSeqAndProceed() {
        // given
        val request = MockHttpServletRequest()
        val session = request.getSession(true)
        session!!.setAttribute("userSeq", 1L)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        // when
        val result = aspect.around(joinPoint, adminRequired)

        // then
        assertThat(result).isEqualTo("OK")
        assertThat(request.getAttribute("userSeq")).isEqualTo(1L)
        verify { joinPoint.proceed() }
    }

    @Test
    @DisplayName("Authorization 헤더도 없고 세션도 없으면 AuthenticationException을 던진다")
    fun noAuthAndNoSession_shouldThrowException() {
        // given
        val request: HttpServletRequest = mockk()
        every { request.getHeader("Authorization") } returns null
        every { request.getSession(false) } returns null
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        // when & then
        assertThatThrownBy { aspect.around(joinPoint, adminRequired) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessage("로그인이 필요합니다.")
        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    @DisplayName("세션은 있지만 userSeq가 없으면 AuthenticationException을 던진다")
    fun sessionWithoutUserSeq_shouldThrowException() {
        // given
        val request: HttpServletRequest = mockk()
        val session: HttpSession = mockk()
        every { request.getHeader("Authorization") } returns null
        every { request.getSession(false) } returns session
        every { session.getAttribute("userSeq") } returns null
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        // when & then
        assertThatThrownBy { aspect.around(joinPoint, adminRequired) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessage("로그인이 필요합니다.")
        verify(exactly = 0) { joinPoint.proceed() }
    }
}

