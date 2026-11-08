package com.walter.lifelog.user.repository

import com.walter.lifelog.shared.store.KeyValueStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest {

    private val keyValueStore: KeyValueStore = mockk(relaxed = true)
    private val expirationDays = 3L
    private val ttl = Duration.ofDays(expirationDays)

    private val refreshTokenRepository = RefreshTokenRepository(keyValueStore, expirationDays)

    @Test
    @DisplayName("save - 이전 토큰이 없으면 새 토큰만 저장한다")
    fun save_shouldSaveNewTokenWhenNoPreviousToken() {
        // given
        val userSeq = 1L
        val token = "newRefreshToken123"
        every { keyValueStore.get("user_refresh_token:$userSeq") } returns null

        // when
        refreshTokenRepository.save(userSeq, token)

        // then
        verify(exactly = 0) { keyValueStore.delete(any()) }
        verify { keyValueStore.set("refresh_token:$token", "1", ttl) }
        verify { keyValueStore.set("user_refresh_token:$userSeq", token, ttl) }
    }

    @Test
    @DisplayName("save - 이전 토큰이 있으면 삭제 후 새 토큰을 저장한다")
    fun save_shouldDeletePreviousTokenAndSaveNewToken() {
        // given
        val userSeq = 1L
        val previousToken = "oldRefreshToken456"
        val newToken = "newRefreshToken789"
        every { keyValueStore.get("user_refresh_token:$userSeq") } returns previousToken

        // when
        refreshTokenRepository.save(userSeq, newToken)

        // then
        verify(exactly = 1) { keyValueStore.delete("refresh_token:$previousToken") }
        verify { keyValueStore.set("refresh_token:$newToken", "1", ttl) }
        verify { keyValueStore.set("user_refresh_token:$userSeq", newToken, ttl) }
    }

    @Test
    @DisplayName("findUserSeqByToken - 유효한 토큰이면 userSeq를 반환한다")
    fun findUserSeqByToken_shouldReturnUserSeqForValidToken() {
        // given
        val token = "validToken123"
        every { keyValueStore.get("refresh_token:$token") } returns "1"

        // when
        val result = refreshTokenRepository.findUserSeqByToken(token)

        // then
        assertThat(result).isEqualTo(1L)
    }

    @Test
    @DisplayName("findUserSeqByToken - 존재하지 않는 토큰이면 null을 반환한다")
    fun findUserSeqByToken_shouldReturnNullForInvalidToken() {
        // given
        val token = "invalidToken"
        every { keyValueStore.get("refresh_token:$token") } returns null

        // when
        val result = refreshTokenRepository.findUserSeqByToken(token)

        // then
        assertThat(result).isNull()
    }
}
