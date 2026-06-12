package com.walter.lifelog.api.util

import com.walter.lifelog.user.config.SessionProperties
import com.walter.lifelog.user.util.SessionCookieHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component

@Component
class SessionLogoutHandler(
    private val sessionCookieHandler: SessionCookieHandler,
    private val redisTemplate: StringRedisTemplate,
    private val sessionProperties: SessionProperties,
) : LogoutHandler {

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        val sessionId = request.cookies
            ?.find { it.name == SessionCookieHandler.COOKIE_NAME }
            ?.value
        if (sessionId != null) {
            redisTemplate.delete("${sessionProperties.keyPrefix}$sessionId")
        }
        sessionCookieHandler.expireSessionCookie(response)
    }
}