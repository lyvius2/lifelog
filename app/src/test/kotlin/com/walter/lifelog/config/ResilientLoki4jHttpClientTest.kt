package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpStatus
import com.github.loki4j.client.http.Loki4jHttpClient
import com.github.loki4j.client.http.LokiResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Duration

@DisplayName("ResilientLoki4jHttpClient 테스트")
class ResilientLoki4jHttpClientTest {

    private val delegate: Loki4jHttpClient = mockk()
    private var now: Long = 0L
    private val retryInterval: Duration = Duration.ofHours(1)

    @Test
    @DisplayName("send - 전송이 계속 성공하면 호출마다 위임한다")
    fun send_alwaysSuccess_delegatesEveryCall() {
        // given
        val sut = createClient(maxAttempts = 3)
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.NO_CONTENT, "")

        // when
        repeat(3) { sut.send(createBatch()) }

        // then
        verify(exactly = 3) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 3번 연속 실패할 때까지는 계속 전송을 시도한다")
    fun send_maxAttemptsThree_keepsTryingUntilThirdFailure() {
        // given
        val sut = createClient(maxAttempts = 3)
        every { delegate.send(any()) } throws IOException("connection refused")

        // when
        repeat(3) { runCatching { sut.send(createBatch()) } }
        repeat(3) { sut.send(createBatch()) }

        // then
        verify(exactly = 3) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 중단 전의 실패는 loki4j 가 보도록 예외를 그대로 전파한다")
    fun send_beforeSuspend_propagatesFailure() {
        // given
        val sut = createClient(maxAttempts = 3)
        every { delegate.send(any()) } throws IOException("connection refused")

        // when / then
        assertThatThrownBy { sut.send(createBatch()) }.isInstanceOf(IOException::class.java)
    }

    @Test
    @DisplayName("send - 중단 상태에서는 전송하지 않고 성공(204)으로 응답한다")
    fun send_whenSuspended_dropsBatchAndReportsSuccess() {
        // given
        val sut = createClient(maxAttempts = 1)
        every { delegate.send(any()) } throws IOException("connection refused")
        runCatching { sut.send(createBatch()) }

        // when
        val response = sut.send(createBatch())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
        verify(exactly = 1) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 2xx 가 아닌 응답도 실패로 센다")
    fun send_nonSuccessStatus_countedAsFailure() {
        // given
        val sut = createClient(maxAttempts = 2)
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.SERVICE_UNAVAILABLE, "down")

        // when
        sut.send(createBatch())
        sut.send(createBatch())
        sut.send(createBatch())

        // then
        verify(exactly = 2) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 임계값에 도달하기 전에 성공하면 연속 실패 횟수가 초기화된다")
    fun send_successBeforeThreshold_resetsFailureCount() {
        // given
        val sut = createClient(maxAttempts = 3)

        // when
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.SERVICE_UNAVAILABLE, "")
        sut.send(createBatch())
        sut.send(createBatch())
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.NO_CONTENT, "")
        sut.send(createBatch())
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.SERVICE_UNAVAILABLE, "")
        repeat(5) { sut.send(createBatch()) }

        // then
        // 실패 2 + 성공 1 로 초기화된 뒤, 다시 3번 실패하고 중단합니다
        verify(exactly = 6) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 재시도 간격이 지나기 전에는 재시도하지 않는다")
    fun send_beforeRetryInterval_doesNotRetry() {
        // given
        val sut = createClient(maxAttempts = 1)
        every { delegate.send(any()) } throws IOException("down")
        runCatching { sut.send(createBatch()) }

        // when
        now += retryInterval.toNanos() - 1
        sut.send(createBatch())

        // then
        verify(exactly = 1) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 재시도 간격이 지나면 1회만 재시도한다")
    fun send_afterRetryInterval_retriesOnlyOnce() {
        // given
        val sut = createClient(maxAttempts = 1)
        every { delegate.send(any()) } throws IOException("down")
        runCatching { sut.send(createBatch()) }

        // when
        now += retryInterval.toNanos()
        repeat(4) { sut.send(createBatch()) }

        // then
        verify(exactly = 2) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 재시도가 성공하면 전송을 재개한다")
    fun send_retrySucceeds_resumesSending() {
        // given
        val sut = createClient(maxAttempts = 1)
        every { delegate.send(any()) } throws IOException("down")
        runCatching { sut.send(createBatch()) }

        // when
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.NO_CONTENT, "")
        now += retryInterval.toNanos()
        sut.send(createBatch())
        sut.send(createBatch())

        // then
        verify(exactly = 3) { delegate.send(any()) }
    }

    @Test
    @DisplayName("send - 재시도가 실패하면 다시 간격만큼 기다린 뒤 1회 재시도한다")
    fun send_retryFails_waitsAnotherInterval() {
        // given
        val sut = createClient(maxAttempts = 1)
        every { delegate.send(any()) } throws IOException("down")
        runCatching { sut.send(createBatch()) }
        now += retryInterval.toNanos()
        sut.send(createBatch())

        // when
        sut.send(createBatch())
        now += retryInterval.toNanos()
        sut.send(createBatch())

        // then
        verify(exactly = 3) { delegate.send(any()) }
    }

    @Test
    @DisplayName("close - 위임 대상의 자원을 정리한다")
    fun close_delegates() {
        // given
        val sut = createClient(maxAttempts = 3)
        every { delegate.close() } returns Unit

        // when
        sut.close()

        // then
        verify(exactly = 1) { delegate.close() }
    }

    private fun createClient(maxAttempts: Int): ResilientLoki4jHttpClient =
        ResilientLoki4jHttpClient(
            delegate = delegate,
            retryInterval = retryInterval,
            maxAttempts = maxAttempts,
            ticker = { now },
        )

    private fun createBatch(): ByteBuffer = ByteBuffer.wrap("log".toByteArray())
}
