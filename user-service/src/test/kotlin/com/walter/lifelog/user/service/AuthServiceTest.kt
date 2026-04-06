package com.walter.lifelog.user.service

import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.repository.RefreshTokenRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("AuthService 테스트")
class AuthServiceTest {

    private val authenticationManager: AuthenticationManager = mockk()
    private val refreshTokenRepository: RefreshTokenRepository = mockk()
    private val jwtSecretKey = "testSecretKey1234567890"

    private val authService = AuthService(authenticationManager, refreshTokenRepository, jwtSecretKey)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("getSecurityContext - 인증 성공 시 SecurityContext에 인증 정보가 설정된다")
    fun getSecurityContext_shouldSetAuthenticationInSecurityContext() {
        // given
        val loginRequest = LoginRequest(email = "admin@lifelog.com", password = "plainPassword")
        val authentication: Authentication = mockk {
            every { isAuthenticated } returns true
            every { name } returns "admin@lifelog.com"
        }

        mockkStatic("com.walter.lifelog.shared.util.RsaKeyHolder")
        every { com.walter.lifelog.shared.util.RsaKeyHolder.decrypt("plainPassword") } returns "decryptedPassword"
        every {
            authenticationManager.authenticate(
                match<UsernamePasswordAuthenticationToken> {
                    it.principal == "admin@lifelog.com" && it.credentials == "decryptedPassword"
                }
            )
        } returns authentication

        // when
        val securityContext = authService.getSecurityContext(loginRequest)

        // then
        assertThat(securityContext.authentication).isEqualTo(authentication)
        verify(exactly = 1) { authenticationManager.authenticate(any()) }

        unmockkStatic("com.walter.lifelog.shared.util.RsaKeyHolder")
    }

    @Test
    @DisplayName("getSecurityContext - 인증 실패 시 예외가 발생한다")
    fun getSecurityContext_shouldThrowExceptionWhenAuthFails() {
        // given
        val loginRequest = LoginRequest(email = "admin@lifelog.com", password = "wrongPassword")

        mockkStatic("com.walter.lifelog.shared.util.RsaKeyHolder")
        every { com.walter.lifelog.shared.util.RsaKeyHolder.decrypt("wrongPassword") } returns "decryptedWrong"
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("인증 실패")

        // when & then
        assertThatThrownBy { authService.getSecurityContext(loginRequest) }
            .isInstanceOf(BadCredentialsException::class.java)

        unmockkStatic("com.walter.lifelog.shared.util.RsaKeyHolder")
    }

    @Test
    @DisplayName("createAccessToken - 액세스 토큰과 리프레시 토큰이 포함된 LoginResponse를 반환한다")
    fun createAccessToken_shouldReturnLoginResponseWithTokens() {
        // given
        val email = "admin@lifelog.com"
        val userSeq = 1L
        val displayName = "Walter"

        every { refreshTokenRepository.save(userSeq, any()) } just Runs

        // when
        val result = authService.createAccessToken(email, userSeq, displayName)

        // then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("로그인 성공")
        assertThat(result.displayName).isEqualTo("Walter")
        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        assertThat(result.accessTokenExpire).isGreaterThan(0)
        verify(exactly = 1) { refreshTokenRepository.save(userSeq, any()) }
    }

    @Test
    @DisplayName("refreshAccessToken - 토큰 갱신 성공 메시지와 새 토큰을 반환한다")
    fun refreshAccessToken_shouldReturnRefreshedTokenResponse() {
        // given
        val userSeq = 1L
        val email = "admin@lifelog.com"
        val displayName = "Walter"

        every { refreshTokenRepository.save(userSeq, any()) } just Runs

        // when
        val result = authService.refreshAccessToken(userSeq, email, displayName)

        // then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("토큰 갱신 성공")
        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        verify(exactly = 1) { refreshTokenRepository.save(userSeq, any()) }
    }

    @Test
    @DisplayName("validateRefreshToken - 유효한 리프레시 토큰이면 userSeq를 반환한다")
    fun validateRefreshToken_shouldReturnUserSeqForValidToken() {
        // given
        val token = "validRefreshToken123"
        every { refreshTokenRepository.findUserSeqByToken(token) } returns 1L

        // when
        val result = authService.validateRefreshToken(token)

        // then
        assertThat(result).isEqualTo(1L)
        verify(exactly = 1) { refreshTokenRepository.findUserSeqByToken(token) }
    }

    @Test
    @DisplayName("validateRefreshToken - 유효하지 않은 리프레시 토큰이면 AuthenticationException이 발생한다")
    fun validateRefreshToken_shouldThrowExceptionForInvalidToken() {
        // given
        val token = "invalidToken"
        every { refreshTokenRepository.findUserSeqByToken(token) } returns null

        // when & then
        assertThatThrownBy { authService.validateRefreshToken(token) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessage("유효하지 않은 리프레시 토큰입니다.")
    }

    @Test
    @DisplayName("getLoginStatus - 인증된 사용자가 있으면 로그인 상태를 반환한다")
    fun getLoginStatus_shouldReturnLoggedInStatusWhenAuthenticated() {
        // given
        val authentication: Authentication = mockk {
            every { isAuthenticated } returns true
            every { principal } returns "admin@lifelog.com"
            every { name } returns "admin@lifelog.com"
        }
        SecurityContextHolder.getContext().authentication = authentication

        // when
        val result = authService.getLoginStatus()

        // then
        assertThat(result.isLoggedIn).isTrue()
        assertThat(result.username).isEqualTo("admin@lifelog.com")
    }

    @Test
    @DisplayName("getLoginStatus - 인증되지 않은 상태이면 비로그인 상태를 반환한다")
    fun getLoginStatus_shouldReturnNotLoggedInWhenNoAuthentication() {
        // given
        SecurityContextHolder.clearContext()

        // when
        val result = authService.getLoginStatus()

        // then
        assertThat(result.isLoggedIn).isFalse()
        assertThat(result.username).isEmpty()
    }

    @Test
    @DisplayName("getLoginStatus - anonymousUser이면 비로그인 상태를 반환한다")
    fun getLoginStatus_shouldReturnNotLoggedInForAnonymousUser() {
        // given
        val authentication: Authentication = mockk {
            every { isAuthenticated } returns true
            every { principal } returns "anonymousUser"
            every { name } returns "anonymousUser"
        }
        SecurityContextHolder.getContext().authentication = authentication

        // when
        val result = authService.getLoginStatus()

        // then
        assertThat(result.isLoggedIn).isFalse()
        assertThat(result.username).isEmpty()
    }
}

