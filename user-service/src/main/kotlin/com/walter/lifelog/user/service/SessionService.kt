package com.walter.lifelog.user.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.user.dto.LifelogSessionAttr
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SessionService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val SESSION_KEY_PREFIX = "spring:session:sessions:"
        private const val SESSION_ATTR_FIELD = "sessionAttr:loginMember"
        private val SESSION_TTL = Duration.ofSeconds(1800)
    }

    fun saveAdminSession(sessionId: String, userId: Long, email: String, username: String) {
        val attr = LifelogSessionAttr(
            userId = userId.toString(),
            email = email,
            username = username,
        )
        val json = objectMapper.writeValueAsString(attr)
        val key = "$SESSION_KEY_PREFIX$sessionId"
        redisTemplate.opsForHash<String, String>().put(key, SESSION_ATTR_FIELD, json)
        redisTemplate.expire(key, SESSION_TTL)
    }

    fun deleteAdminSession(sessionId: String) {
        redisTemplate.delete("$SESSION_KEY_PREFIX$sessionId")
    }
}