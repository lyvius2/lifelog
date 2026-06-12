package com.walter.lifelog.api.config

import com.walter.lifelog.user.config.SessionProperties
import com.walter.lifelog.user.util.SessionCookieHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SessionTtlInterceptor(
    private val redisTemplate: StringRedisTemplate,
    private val sessionProperties: SessionProperties,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val sessionId = request.cookies?.find { it.name == SessionCookieHandler.COOKIE_NAME }?.value
        if (sessionId != null) {
            redisTemplate.expire("${sessionProperties.keyPrefix}$sessionId", sessionProperties.ttl)
        }
        return true
    }
}