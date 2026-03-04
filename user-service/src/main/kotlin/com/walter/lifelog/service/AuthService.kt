package com.walter.lifelog.service

import com.walter.lifelog.dto.LoginRequest
import com.walter.lifelog.dto.LoginResponse
import com.walter.lifelog.dto.LoginStatusResponse
import com.walter.lifelog.repository.UserRepository
import com.walter.lifelog.util.AccessTokenHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    fun authenticate(loginRequest: LoginRequest, httpServletRequest: HttpServletRequest): LoginResponse {
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        )
        val securityContext = SecurityContextHolder.getContext()
        securityContext.authentication = authentication

        val session: HttpSession = httpServletRequest.getSession(true)
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
        session.maxInactiveInterval = 1800

        val user = userRepository.findByEmail(loginRequest.email)
        if (user != null) {
            session.setAttribute("userSeq", user.userSeq)
        }

        val accessToken = AccessTokenHandler.generateToken(loginRequest.email, jwtSecretKey)
        return LoginResponse.ok(authentication.name, accessToken)
    }

    fun getLoginStatus(): LoginStatusResponse {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        val isLoggedIn = authentication != null
                && authentication.isAuthenticated
                && authentication.principal != "anonymousUser"
        val username = if (isLoggedIn) authentication.name else ""

        return LoginStatusResponse(isLoggedIn, username)
    }
}