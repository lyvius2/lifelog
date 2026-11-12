package com.walter.lifelog.api.config.aop

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.config.exception.UnauthorizedException
import com.walter.lifelog.shared.util.AdminAccessResolver
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
        val authorization = request.getHeader("Authorization")
        if (!authorization.isNullOrBlank()) {
            return AdminAccessResolver.resolveFromToken(authorization, jwtSecretKey)
                .orElseThrow { UnauthorizedException("잘못된 토큰입니다.") }
        }
        val sessionUserSeq = request.getSession(false)?.getAttribute("userSeq")
        return AdminAccessResolver.resolveFromSession(sessionUserSeq)
            .orElseThrow { UnauthorizedException("로그인이 필요합니다.") }
    }
}

