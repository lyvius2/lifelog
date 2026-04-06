package com.walter.lifelog.user.service

import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.util.TokenHandler
import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.apache.commons.lang3.StringUtils
import org.springframework.security.core.context.SecurityContext

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    fun getSecurityContext(loginRequest: LoginRequest): SecurityContext {
        val decryptedPassword = RsaKeyHolder.decrypt(loginRequest.password)
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.email, decryptedPassword)
        )
        val securityContext = SecurityContextHolder.getContext()
        securityContext.authentication = authentication
        return securityContext
    }

    fun createAccessToken(email: String, userSeq: Long, displayName: String): LoginResponse {
        val accessToken = TokenHandler.generateAccessToken(email, userSeq, displayName, jwtSecretKey)
        val refreshToken = createRefreshToken(userSeq)
        return LoginResponse.of(displayName, "로그인 성공", accessToken, refreshToken)
    }

    fun refreshAccessToken(userSeq: Long, email: String, displayName: String): LoginResponse {
        return createAccessToken(email, userSeq, displayName).apply { message = "토큰 갱신 성공" }
    }

    fun validateRefreshToken(token: String): Long {
        return refreshTokenRepository.findUserSeqByToken(token) ?: throw AuthenticationException("유효하지 않은 리프레시 토큰입니다.")
    }

    private fun createRefreshToken(userSeq: Long): String {
        val tokenValue = TokenHandler.generateRefreshToken()
        refreshTokenRepository.save(userSeq, tokenValue)
        return tokenValue
    }

    fun getLoginStatus(): LoginStatusResponse {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        val isLoggedIn = authentication != null
                && authentication.isAuthenticated
                && authentication.principal != "anonymousUser"
        val username = if (isLoggedIn) authentication.name else StringUtils.EMPTY
        return LoginStatusResponse.of(isLoggedIn, username)
    }
}