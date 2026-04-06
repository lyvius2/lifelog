package com.walter.lifelog.user.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest {

    private val valueOperations: ValueOperations<String, String> = mockk(relaxed = true)
    private val redisTemplate: StringRedisTemplate = mockk {
        every { opsForValue() } returns valueOperations
        every { delete(any<String>()) } returns true
    }
    private val expirationDays = 3L

    private val refreshTokenRepository = RefreshTokenRepository(redisTemplate, expirationDays)

    @Test
    @DisplayName("save - 이전 토큰이 없으면 새 토큰만 저장한다")
    fun save_shouldSaveNewTokenWhenNoPreviousToken() {
        // given
        val userSeq = 1L
        val token = "newRefreshToken123"
        every { valueOperations.get("user_refresh_token:$userSeq") } returns null

        // when
        refreshTokenRepository.save(userSeq, token)

        // then
        verify(exactly = 0) { redisTemplate.delete(any<String>()) }
        verify { valueOperations.set("refresh_token:$token", "1", Duration.ofDays(3)) }
        verify { valueOperations.set("user_refresh_token:$userSeq", token, Duration.ofDays(3)) }
    }

    @Test
    @DisplayName("save - 이전 토큰이 있으면 삭제 후 새 토큰을 저장한다")
    fun save_shouldDeletePreviousTokenAndSaveNewToken() {
        // given
        val userSeq = 1L
        val previousToken = "oldRefreshToken456"
        val newToken = "newRefreshToken789"
        every { valueOperations.get("user_refresh_token:$userSeq") } returns previousToken

        // when
        refreshTokenRepository.save(userSeq, newToken)

        // then
        verify(exactly = 1) { redisTemplate.delete("refresh_token:$previousToken") }
        verify { valueOperations.set("refresh_token:$newToken", "1", Duration.ofDays(3)) }
        verify { valueOperations.set("user_refresh_token:$userSeq", newToken, Duration.ofDays(3)) }
    }

    @Test
    @DisplayName("findUserSeqByToken - 유효한 토큰이면 userSeq를 반환한다")
    fun findUserSeqByToken_shouldReturnUserSeqForValidToken() {
        // given
        val token = "validToken123"
        every { valueOperations.get("refresh_token:$token") } returns "1"

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
        every { valueOperations.get("refresh_token:$token") } returns null

        // when
        val result = refreshTokenRepository.findUserSeqByToken(token)

        // then
        assertThat(result).isNull()
    }
}

