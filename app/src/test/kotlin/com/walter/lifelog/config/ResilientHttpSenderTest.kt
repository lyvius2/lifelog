package com.walter.lifelog.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ResilientHttpSender 테스트")
class ResilientHttpSenderTest {

    @Test
    @DisplayName("getHttpClientFactory - 기본값은 ResilientLoki4jHttpClient 로 감싼다")
    fun getHttpClientFactory_default_wrapsClient() {
        // given
        val sut = createSender()

        // when
        val client = sut.httpClientFactory.apply(sut.config.build("java"))

        // then
        assertThat(client).isInstanceOf(ResilientLoki4jHttpClient::class.java)
    }

    @Test
    @DisplayName("getHttpClientFactory - resilient 가 false 면 감싸지 않는다")
    fun getHttpClientFactory_resilientDisabled_doesNotWrap() {
        // given
        val sut = createSender().apply { resilient = false }

        // when
        val client = sut.httpClientFactory.apply(sut.config.build("java"))

        // then
        assertThat(client).isNotInstanceOf(ResilientLoki4jHttpClient::class.java)
    }

    @Test
    @DisplayName("기본값 - maxAttempts 3, retryIntervalMs 1시간, probeTimeoutMs 2초")
    fun defaults_areThreeAttemptsAndOneHour() {
        // given / when
        val sut = createSender()

        // then
        assertThat(sut.maxAttempts).isEqualTo(3)
        assertThat(sut.retryIntervalMs).isEqualTo(3_600_000L)
        assertThat(sut.probeTimeoutMs).isEqualTo(2_000L)
        assertThat(sut.resilient).isTrue()
    }

    private fun createSender(): ResilientHttpSender =
        ResilientHttpSender().apply { url = "http://localhost:3100/loki/api/v1/push" }
}
