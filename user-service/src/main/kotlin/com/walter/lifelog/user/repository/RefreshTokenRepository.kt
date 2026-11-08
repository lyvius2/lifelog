package com.walter.lifelog.user.repository

import com.walter.lifelog.shared.store.KeyValueStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RefreshTokenRepository(
    private val keyValueStore: KeyValueStore,
    @Value("\${jwt.refresh-token-expiration-days:3}") private val expirationDays: Long,
) {
    companion object {
        private const val TOKEN_KEY_PREFIX = "refresh_token:"
        private const val USER_TOKEN_KEY_PREFIX = "user_refresh_token:"
    }

    fun save(userSeq: Long, token: String) {
        val previousToken = keyValueStore.get("$USER_TOKEN_KEY_PREFIX$userSeq")
        if (previousToken != null) {
            keyValueStore.delete("$TOKEN_KEY_PREFIX$previousToken")
        }
        val ttl = Duration.ofDays(expirationDays)
        keyValueStore.set("$TOKEN_KEY_PREFIX$token", userSeq.toString(), ttl)
        keyValueStore.set("$USER_TOKEN_KEY_PREFIX$userSeq", token, ttl)
    }

    fun findUserSeqByToken(token: String): Long? {
        return keyValueStore.get("$TOKEN_KEY_PREFIX$token")?.toLong()
    }

    fun deleteByUserSeq(userSeq: Long) {
        keyValueStore.get("$USER_TOKEN_KEY_PREFIX$userSeq")?.let { token ->
            keyValueStore.delete("$TOKEN_KEY_PREFIX$token")
        }
        keyValueStore.delete("$USER_TOKEN_KEY_PREFIX$userSeq")
    }
}
