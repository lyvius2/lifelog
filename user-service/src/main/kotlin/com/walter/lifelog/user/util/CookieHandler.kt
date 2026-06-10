package com.walter.lifelog.user.util

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CookieHandler {

    companion object {
        const val COOKIE_NAME = "LIFELOG_SESSION"
        private val SESSION_TTL = Duration.ofSeconds(1800)
    }

    fun addSessionCookie(response: HttpServletResponse, sessionId: String) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(sessionId, SESSION_TTL).toString())
    }

    fun expireSessionCookie(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString())
    }

    private fun buildCookie(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build()
}