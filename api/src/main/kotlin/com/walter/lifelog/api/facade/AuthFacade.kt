package com.walter.lifelog.api.facade

import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.UserService
import jakarta.servlet.http.HttpSession

@Facade
class AuthFacade(
    private val authService: AuthService,
    private val userService: UserService,
) {
    fun executeAuthenticate(loginRequest: LoginRequest, httpSession: HttpSession): LoginResponse {
        val securityContext = authService.getSecurityContext(loginRequest)
        httpSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
        httpSession.maxInactiveInterval = 1800

        val email = loginRequest.email
        val userSimpleInfo = userService.getUserSimpleInfo(email)
        httpSession.setAttribute("userSeq", userSimpleInfo.userSeq)
        val loginResponse = authService.createAccessToken(email, userSimpleInfo.userSeq, userSimpleInfo.displayName ?: "")
        return loginResponse
    }
}