package com.walter.lifelog.user.service

import com.walter.lifelog.shared.util.AccessTokenHandler
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
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
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    fun getSecurityContext(loginRequest: LoginRequest): SecurityContext {
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        )
        val securityContext = SecurityContextHolder.getContext()
        securityContext.authentication = authentication
        return securityContext
    }

    fun createAccessToken(email: String, userSeq: Long, displayName: String): LoginResponse {
        val accessToken = AccessTokenHandler.generateToken(
            email,
            userSeq,
            displayName,
            jwtSecretKey
        )
        return LoginResponse.of(displayName, accessToken)
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