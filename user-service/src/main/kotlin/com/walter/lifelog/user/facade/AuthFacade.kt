package com.walter.lifelog.user.facade

import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.dto.RefreshRequest
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.BadCredentialsException

@Facade
class AuthFacade(
    private val authService: AuthService,
    private val userService: UserService,
) {
    fun executeAuthenticate(loginRequest: LoginRequest, httpSession: HttpSession): LoginResponse {
        try {
            val securityContext = authService.getSecurityContext(loginRequest)
            httpSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
            httpSession.maxInactiveInterval = 1800

            val email = loginRequest.email
            val userSimpleInfo = userService.getUserSimpleInfo(email)
            httpSession.setAttribute("userSeq", userSimpleInfo.userSeq)
            return authService.createAccessToken(email, userSimpleInfo.userSeq, userSimpleInfo.displayName ?: "")
        } catch (_: BadCredentialsException) {
            throw AuthenticationException()
        } catch (e: Exception) {
            throw LoginException(e)
        }
    }

    fun refreshToken(refreshRequest: RefreshRequest): LoginResponse {
        try {
            val userSeq = authService.validateRefreshToken(refreshRequest.refreshToken)
            val author = userService.getAuthorInfoByUserSeq(userSeq)
            return authService.refreshAccessToken(userSeq, author.email!!, author.name!!)
        } catch (e: Exception) {
            throw LoginException(e)
        }
    }

    fun getLoginStatus(): LoginStatusResponse {
        return authService.getLoginStatus()
    }
}