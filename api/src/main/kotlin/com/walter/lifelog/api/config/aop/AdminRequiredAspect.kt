package com.walter.lifelog.api.config.aop

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.shared.util.TokenHandler
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class AdminRequiredAspect(
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String
) {
    @Around("@annotation(adminRequired)")
    fun around(joinPoint: ProceedingJoinPoint, adminRequired: AdminRequired): Any? {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val userSeq = resolveUserSeq(request)
        request.setAttribute("userSeq", userSeq)
        return joinPoint.proceed()
    }

    private fun resolveUserSeq(request: HttpServletRequest): Long {
        return request.getHeader("Authorization")
            ?.takeIf { it.isNotBlank() }
            ?.let { resolveFromToken(it) }
            ?: resolveFromSession(request)
    }

    private fun resolveFromToken(authorization: String): Long {
        return runCatching { TokenHandler.getUserSeqFromToken(authorization, jwtSecretKey) }
            .getOrNull() ?: throw LoginException(IllegalStateException("잘못된 토큰입니다."))
    }

    private fun resolveFromSession(request: HttpServletRequest): Long {
        val session = request.getSession(false) ?: throw AuthenticationException("로그인이 필요합니다.")
        return when (val value = session.getAttribute("userSeq")) {
            is Long -> value
            is Number -> value.toLong()
            else -> throw AuthenticationException("로그인이 필요합니다.")
        }
    }
}

