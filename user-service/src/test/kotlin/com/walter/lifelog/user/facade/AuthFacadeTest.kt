package com.walter.lifelog.user.facade

import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.dto.RefreshRequest
import com.walter.lifelog.user.dto.UserSimpleInfo
import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContext

class AuthFacadeTest {

    private val authService: AuthService = mockk()
    private val userService: UserService = mockk()
    private val authFacade = AuthFacade(authService, userService)

    @Test
    @DisplayName("executeAuthenticate - 인증 성공 시 세션에 SecurityContext와 userSeq를 저장하고 LoginResponse를 반환한다")
    fun executeAuthenticate_shouldSetSessionAndReturnLoginResponse() {
        // given
        val loginRequest = LoginRequest(email = "test@example.com", password = "encryptedPassword")
        val httpSession: HttpSession = mockk(relaxed = true)
        val securityContext: SecurityContext = mockk()
        val userSimpleInfo = UserSimpleInfo(userSeq = 1L, displayName = "Walter", name = "walter")
        val expectedResponse = LoginResponse.of("Walter", "로그인 성공", "generated-access-token", "generated-refresh-token")

        every { authService.getSecurityContext(loginRequest) } returns securityContext
        every { userService.getUserSimpleInfo("test@example.com") } returns userSimpleInfo
        every { authService.createAccessToken("test@example.com", 1L, "Walter") } returns expectedResponse

        // when
        val result = authFacade.executeAuthenticate(loginRequest, httpSession)

        // then
        assertThat(result.success).isTrue()
        assertThat(result.displayName).isEqualTo("Walter")
        assertThat(result.accessToken).isEqualTo("generated-access-token")
        assertThat(result.refreshToken).isEqualTo("generated-refresh-token")

        verify { httpSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext) }
        verify { httpSession.maxInactiveInterval = 1800 }
        verify { httpSession.setAttribute("userSeq", 1L) }
        verify { authService.getSecurityContext(loginRequest) }
        verify { userService.getUserSimpleInfo("test@example.com") }
        verify { authService.createAccessToken("test@example.com", 1L, "Walter") }
    }

    @Test
    @DisplayName("executeAuthenticate - displayName이 null이면 빈 문자열로 accessToken을 생성한다")
    fun executeAuthenticate_shouldUseEmptyStringWhenDisplayNameIsNull() {
        // given
        val loginRequest = LoginRequest(email = "no-name@example.com", password = "encryptedPassword")
        val httpSession: HttpSession = mockk(relaxed = true)
        val securityContext: SecurityContext = mockk()
        val userSimpleInfo = UserSimpleInfo(userSeq = 2L, displayName = null, name = "noname")
        val expectedResponse = LoginResponse.of("", "로그인 성공", "token-no-name", "refresh-token-no-name")

        every { authService.getSecurityContext(loginRequest) } returns securityContext
        every { userService.getUserSimpleInfo("no-name@example.com") } returns userSimpleInfo
        every { authService.createAccessToken("no-name@example.com", 2L, "") } returns expectedResponse

        // when
        val result = authFacade.executeAuthenticate(loginRequest, httpSession)

        // then
        assertThat(result.success).isTrue()
        assertThat(result.displayName).isEmpty()
        verify { authService.createAccessToken("no-name@example.com", 2L, "") }
    }

    @Test
    @DisplayName("executeAuthenticate - 잘못된 자격 증명 시 AuthenticationException을 던진다")
    fun executeAuthenticate_shouldThrowAuthenticationExceptionWhenBadCredentials() {
        // given
        val loginRequest = LoginRequest(email = "test@example.com", password = "wrongPassword")
        val httpSession: HttpSession = mockk(relaxed = true)

        every { authService.getSecurityContext(loginRequest) } throws BadCredentialsException("Bad credentials")

        // when & then
        assertThatThrownBy { authFacade.executeAuthenticate(loginRequest, httpSession) }
            .isInstanceOf(AuthenticationException::class.java)
    }

    @Test
    @DisplayName("refreshToken - 유효한 리프레시 토큰으로 새로운 토큰을 발급한다")
    fun refreshToken_shouldReturnNewTokensWhenRefreshTokenIsValid() {
        // given
        val refreshRequest = RefreshRequest(refreshToken = "valid-refresh-token")
        val author = Author(
            email = "test@example.com",
            name = "Walter",
            bio = null,
            profileImageUrl = null,
            githubUrl = null,
            linkedinUrl = null
        )
        val expectedResponse = LoginResponse.of("Walter", "토큰 갱신 성공", "new-access-token", "new-refresh-token")

        every { authService.validateRefreshToken("valid-refresh-token") } returns 1L
        every { userService.getAuthorInfoByUserSeq(1L) } returns author
        every { authService.refreshAccessToken(1L, "test@example.com", "Walter") } returns expectedResponse

        // when
        val result = authFacade.refreshToken(refreshRequest)

        // then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("토큰 갱신 성공")
        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")

        verify { authService.validateRefreshToken("valid-refresh-token") }
        verify { userService.getAuthorInfoByUserSeq(1L) }
        verify { authService.refreshAccessToken(1L, "test@example.com", "Walter") }
    }

    @Test
    @DisplayName("refreshToken - 유효하지 않은 리프레시 토큰은 LoginException을 던진다")
    fun refreshToken_shouldThrowLoginExceptionWhenRefreshTokenIsInvalid() {
        // given
        val refreshRequest = RefreshRequest(refreshToken = "invalid-refresh-token")

        every { authService.validateRefreshToken("invalid-refresh-token") } throws AuthenticationException("유효하지 않은 리프레시 토큰입니다.")

        // when & then
        assertThatThrownBy { authFacade.refreshToken(refreshRequest) }
            .isInstanceOf(LoginException::class.java)

        verify { authService.validateRefreshToken("invalid-refresh-token") }
        verify(exactly = 0) { userService.getAuthorInfoByUserSeq(any()) }
    }

    @Test
    @DisplayName("getLoginStatus - 로그인 상태를 반환한다")
    fun getLoginStatus_shouldReturnLoginStatusResponse() {
        // given
        val expectedResponse = LoginStatusResponse.of(isLoggedIn = true, username = "test@example.com")
        every { authService.getLoginStatus() } returns expectedResponse

        // when
        val result = authFacade.getLoginStatus()

        // then
        assertThat(result.isLoggedIn).isTrue()
        assertThat(result.username).isEqualTo("test@example.com")
        verify { authService.getLoginStatus() }
    }

    @Test
    @DisplayName("getLoginStatus - 비로그인 상태를 반환한다")
    fun getLoginStatus_shouldReturnLoggedOutStatus() {
        // given
        val expectedResponse = LoginStatusResponse.of(isLoggedIn = false, username = "")
        every { authService.getLoginStatus() } returns expectedResponse

        // when
        val result = authFacade.getLoginStatus()

        // then
        assertThat(result.isLoggedIn).isFalse()
        assertThat(result.username).isEmpty()
        verify { authService.getLoginStatus() }
    }
}

