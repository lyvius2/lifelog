package com.walter.lifelog.user.facade

import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.dto.RefreshRequest
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.SessionService
import com.walter.lifelog.user.service.UserService
import com.walter.lifelog.user.util.CookieHandler
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.BadCredentialsException

@Facade
class AuthFacade(
    private val authService: AuthService,
    private val userService: UserService,
    private val sessionService: SessionService,
    private val cookieHandler: CookieHandler,
) {
    fun executeAuthenticate(loginRequest: LoginRequest, httpSession: HttpSession, response: HttpServletResponse): LoginResponse {
        try {
            val securityContext = authService.getSecurityContext(loginRequest)
            httpSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
            httpSession.maxInactiveInterval = 1800

            val email = loginRequest.email
            val userSimpleInfo = userService.getUserSimpleInfo(email)
            val userDisplayName = userSimpleInfo.displayName ?: ""
            httpSession.setAttribute("userSeq", userSimpleInfo.userSeq)
            sessionService.saveAdminSession(httpSession.id, userSimpleInfo.userSeq, email, userDisplayName)
            cookieHandler.addSessionCookie(response, httpSession.id)
            return authService.createAccessToken(email, userSimpleInfo.userSeq, userDisplayName)
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

    fun executeLogout(httpSession: HttpSession?, response: HttpServletResponse) {
        if (httpSession != null) {
            sessionService.deleteAdminSession(httpSession.id)
            httpSession.invalidate()
        }
        cookieHandler.expireSessionCookie(response)
    }

    fun getLoginStatus(): LoginStatusResponse {
        return authService.getLoginStatus()
    }
}