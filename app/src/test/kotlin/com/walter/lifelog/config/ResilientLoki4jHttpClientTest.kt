package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpStatus
import com.github.loki4j.client.http.Loki4jHttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.github.loki4j.client.http.LokiResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

@DisplayName("ResilientLoki4jHttpClient 테스트")
class ResilientLoki4jHttpClientTest {

    private val delegate: Loki4jHttpClient = mockk()
    private val suspension: ExportSuspensionHandler = mockk(relaxed = true)
    private val sut = ResilientLoki4jHttpClient(delegate = delegate, suspension = suspension)

    @Test
    @DisplayName("send - 전송 중 상태에서는 호출마다 위임한다")
    fun send_whenSending_delegatesEveryCall() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.send(any()) } returns LokiResponse(HttpStatus.NO_CONTENT, "")

        // when
        repeat(3) { sut.send(createBatch()) }

        // then
        verify(exactly = 3) { delegate.send(any()) }
        verify(exactly = 3) { suspension.onSuccess() }
    }

    @Test
    @DisplayName("send - 2xx 가 아닌 응답은 실패로 알린다")
    fun send_nonSuccessStatus_notifiesFailure() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.send(any()) } returns LokiResponse(503, "service unavailable")

        // when
        sut.send(createBatch())

        // then
        verify(exactly = 1) { suspension.onFailure("HTTP 503") }
        verify(exactly = 0) { suspension.onSuccess() }
    }

    @Test
    @DisplayName("send - 중단 전의 실패는 loki4j 가 보도록 예외를 그대로 전파한다")
    fun send_beforeSuspend_propagatesFailure() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.send(any()) } throws IOException("connection refused")

        // when / then
        assertThatThrownBy { sut.send(createBatch()) }.isInstanceOf(IOException::class.java)
        verify(exactly = 1) { suspension.onFailure(any<Throwable>()) }
    }

    @Test
    @DisplayName("send - 중단 상태에서는 전송하지 않고 성공(204)으로 응답한다")
    fun send_whenSuspended_dropsBatchAsSuccess() {
        // given
        every { suspension.isSuspended } returns true

        // when
        val response = sut.send(createBatch())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
        verify(exactly = 0) { delegate.send(any()) }
    }

    @Test
    @DisplayName("close - 연결 확인 스케줄러와 위임 대상을 모두 정리한다")
    fun close_closesHandlerAndDelegate() {
        // given
        every { delegate.close() } returns Unit

        // when
        sut.close()

        // then
        verify(exactly = 1) { suspension.close() }
        verify(exactly = 1) { delegate.close() }
    }

    @Test
    @DisplayName("send - 연속 실패가 maxAttempts 에 도달하면 이후 전송을 건너뛴다")
    fun send_reachesMaxAttempts_stopsDelegating() {
        // given
        val scheduler: ScheduledExecutorService = mockk(relaxed = true)
        val client = ResilientLoki4jHttpClient(
            delegate = delegate,
            suspension = ExportSuspensionHandler(
                subject = "Loki 로그",
                maxAttempts = 3,
                retryInterval = Duration.ofHours(1),
                probe = { false },
                scheduler = scheduler,
            ),
        )
        every { delegate.send(any()) } throws IOException("connection refused")

        // when
        repeat(10) { runCatching { client.send(createBatch()) } }

        // then
        verify(exactly = 3) { delegate.send(any()) }
    }

    private fun createBatch(): ByteBuffer = ByteBuffer.wrap("log".toByteArray())
}
