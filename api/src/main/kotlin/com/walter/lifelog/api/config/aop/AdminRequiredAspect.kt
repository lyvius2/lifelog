package com.walter.lifelog.api.config.aop

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.util.AccessTokenHandler
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
        val attributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val request = attributes.request
        val authorization = request.getHeader("Authorization")
        val userSeq: Long
        if (!authorization.isNullOrBlank()) {
            userSeq = try {
                AccessTokenHandler.getUserSeqFromToken(authorization, jwtSecretKey)
                    ?: throw IllegalStateException("잘못된 토큰입니다.")
            } catch (e: Exception) {
                throw IllegalStateException("잘못된 토큰입니다.")
            }
        } else {
            val session = request.getSession(false)
                ?: throw IllegalStateException("로그인이 필요합니다.")
            val sessionUserSeq = session.getAttribute("userSeq")
            userSeq = when (sessionUserSeq) {
                is Long -> sessionUserSeq
                is Number -> sessionUserSeq.toLong()
                else -> throw IllegalStateException("로그인이 필요합니다.")
            }
        }
        request.setAttribute("userSeq", userSeq)
        return joinPoint.proceed()
    }
}

