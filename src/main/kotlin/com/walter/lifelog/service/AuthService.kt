package com.walter.lifelog.service

import com.walter.lifelog.controller.dto.LoginRequest
import com.walter.lifelog.controller.dto.LoginResponse
import com.walter.lifelog.controller.dto.LoginStatusResponse
import com.walter.lifelog.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
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
        return LoginResponse.ok(authentication.name)
    }

    fun getLoginStatus(): LoginStatusResponse {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        val isLoggedIn = authentication != null
                && authentication.isAuthenticated
                && authentication.principal != "anonymousUser"
        val username = if (isLoggedIn && authentication != null) authentication.name else ""

        return LoginStatusResponse(isLoggedIn, username)
    }
}