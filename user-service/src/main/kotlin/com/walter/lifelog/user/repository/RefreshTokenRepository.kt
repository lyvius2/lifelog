package com.walter.lifelog.user.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RefreshTokenRepository(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${jwt.refresh-token-expiration-days:3}") private val expirationDays: Long,
) {
    companion object {
        private const val TOKEN_KEY_PREFIX = "refresh_token:"
        private const val USER_TOKEN_KEY_PREFIX = "user_refresh_token:"
    }

    fun save(userSeq: Long, token: String) {
        val previousToken = redisTemplate.opsForValue().get("$USER_TOKEN_KEY_PREFIX$userSeq")
        if (previousToken != null) {
            redisTemplate.delete("$TOKEN_KEY_PREFIX$previousToken")
        }
        redisTemplate.opsForValue().set("$TOKEN_KEY_PREFIX$token", userSeq.toString(), Duration.ofDays(expirationDays))
        redisTemplate.opsForValue().set("$USER_TOKEN_KEY_PREFIX$userSeq", token, Duration.ofDays(expirationDays))
    }

    fun findUserSeqByToken(token: String): Long? {
        return redisTemplate.opsForValue().get("$TOKEN_KEY_PREFIX$token")?.toLong()
    }
}
