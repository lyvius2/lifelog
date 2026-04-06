package com.walter.lifelog.user.service

import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.util.TokenHandler
import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.entity.RefreshToken
import com.walter.lifelog.user.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.apache.commons.lang3.StringUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
    @Value("\${jwt.refresh-token-expiration-days:3}") private val refreshTokenExpirationDays: Long,
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

    @Transactional
    fun createAccessToken(email: String, userSeq: Long, displayName: String): LoginResponse {
        val accessToken = TokenHandler.generateAccessToken(email, userSeq, displayName, jwtSecretKey)
        val refreshToken = createRefreshToken(userSeq)
        return LoginResponse.of(displayName, "로그인 성공", accessToken, refreshToken)
    }

    @Transactional
    fun refreshAccessToken(userSeq: Long, email: String, displayName: String): LoginResponse {
        val accessToken = TokenHandler.generateAccessToken(email, userSeq, displayName, jwtSecretKey)
        val refreshToken = createRefreshToken(userSeq)
        return LoginResponse.of(displayName, "토큰 갱신 성공", accessToken, refreshToken)
    }

    @Transactional(readOnly = true)
    fun validateRefreshToken(token: String): Long {
        val refreshToken = refreshTokenRepository.findByTokenAndIsActiveTrue(token) ?: throw AuthenticationException("유효하지 않은 리프레시 토큰입니다.")
        if (refreshToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw AuthenticationException("유효하지 않은 리프레시 토큰입니다.")
        }
        return refreshToken.userSeq
    }

    private fun createRefreshToken(userSeq: Long): String {
        val tokenValue = TokenHandler.generateRefreshToken()
        val expiresAt = LocalDateTime.now().plusDays(refreshTokenExpirationDays)
        val refreshToken = RefreshToken(
            userSeq = userSeq,
            token = tokenValue,
            expiresAt = expiresAt,
            isActive = true
        )
        refreshTokenRepository.invalidateAllByUserSeq(userSeq)
        refreshTokenRepository.save(refreshToken)
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