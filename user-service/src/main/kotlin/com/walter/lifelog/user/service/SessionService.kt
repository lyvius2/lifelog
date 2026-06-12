package com.walter.lifelog.user.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.walter.lifelog.user.config.SessionProperties
import com.walter.lifelog.user.dto.LifelogSessionAttr
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class SessionService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val sessionProperties: SessionProperties,
) {
    fun saveAdminSession(sessionId: String, userId: Long, email: String, username: String) {
        val attr = LifelogSessionAttr(
            userId = userId.toString(),
            email = email,
            username = username,
        )
        val json = objectMapper.writeValueAsString(attr)
        val key = "${sessionProperties.keyPrefix}$sessionId"
        redisTemplate.opsForHash<String, String>().put(key, sessionProperties.attrField, json)
        redisTemplate.expire(key, sessionProperties.ttl)
    }

    fun deleteAdminSession(sessionId: String) {
        redisTemplate.delete("${sessionProperties.keyPrefix}$sessionId")
    }

    fun extendSessionTtl(sessionId: String) {
        redisTemplate.expire("${sessionProperties.keyPrefix}$sessionId", sessionProperties.ttl)
    }
}