package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpConfig
import com.github.loki4j.client.http.HttpStatus
import com.github.loki4j.client.http.Loki4jHttpClient
import com.github.loki4j.client.http.LokiResponse
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 로그 전송이 [maxAttempts] 번 연속 실패하면 전송을 중단하고, [retryInterval] 마다
 * 1회만 재시도하는 [Loki4jHttpClient] 데코레이터.
 *
 * Trace 쪽 [ResilientSpanExporter] 와 같은 정책을 로그에도 적용한다. Loki 가 떠 있지
 * 않으면 loki4j 가 배치마다 전송을 시도하며 오류를 끝없이 쏟아내는데, 그것을 막는다.
 *
 * - **전송 중**: 평소처럼 위임한다. 실패하면 연속 실패를 세고, [maxAttempts] 에
 *   도달하면 중단 상태로 넘어간다. 한 번이라도 성공하면 카운터는 0 으로 돌아간다.
 *   이 구간의 실패는 그대로 loki4j 에 전달해 평소의 오류 로그가 보이게 둔다.
 * - **중단**: 전송하지 않고 배치를 버리며, loki4j 가 실패로 보지 않도록
 *   [HttpStatus.NO_CONTENT] 로 응답한다(Loki 의 정상 응답과 같은 코드). 그래서
 *   중단 이후로는 오류 로그가 더 쌓이지 않는다. [retryInterval] 이 지나면 1회만
 *   위임해 보고, 성공하면 전송을 재개한다.
 *
 * ## 로깅 채널
 * appender 내부에서 돌지만 SLF4J 를 쓴다. `logback-spring.xml` 이
 * `NopStatusListener` 를 걸어 두어 logback status 채널은 출력되지 않기 때문이다.
 * loki4j 의 파이프라인은 비동기(AsyncBufferPipeline)라 이 스레드에서 남긴 로그가
 * [send] 를 동기적으로 다시 호출하지 않으며, 중단 상태에서 버리는 배치는 로그를
 * 남기지 않으므로 되먹임도 생기지 않는다.
 */
class ResilientLoki4jHttpClient(
    private val delegate: Loki4jHttpClient,
    private val retryInterval: Duration,
    private val maxAttempts: Int = 3,
    private val ticker: () -> Long = System::nanoTime,
) : Loki4jHttpClient {

    companion object {
        private val log = LoggerFactory.getLogger(ResilientLoki4jHttpClient::class.java)

        /** 중단 상태에서 돌려주는 응답. Loki 가 성공 시 주는 코드와 같다. */
        private val DROPPED = LokiResponse(HttpStatus.NO_CONTENT, "")

        private fun isSuccess(response: LokiResponse): Boolean = response.status in 200..299
    }

    /** 1 미만이 들어오면 "첫 실패에 중단" 으로 본다. */
    private val attemptLimit = maxAttempts.coerceAtLeast(1)

    private val suspended = AtomicBoolean(false)
    private val retrying = AtomicBoolean(false)
    private val nextRetryAt = AtomicLong(0)
    private val consecutiveFailures = AtomicInteger(0)

    override fun getConfig(): HttpConfig = delegate.config

    override fun send(batch: ByteBuffer): LokiResponse {
        if (!suspended.get()) {
            return sendNormally(batch)
        }
        if (ticker() - nextRetryAt.get() < 0) {
            return DROPPED
        }
        if (!retrying.compareAndSet(false, true)) {
            return DROPPED
        }
        return try {
            sendAsRetry(batch)
        } finally {
            retrying.set(false)
        }
    }

    override fun close() = delegate.close()

    private fun sendNormally(batch: ByteBuffer): LokiResponse {
        val response = try {
            delegate.send(batch)
        } catch (e: Exception) {
            countFailure(describe(e))
            throw e // 중단 전까지는 loki4j 가 실패를 그대로 보게 둡니다
        }
        if (isSuccess(response)) {
            consecutiveFailures.set(0)
        } else {
            countFailure("HTTP ${response.status}")
        }
        return response
    }

    private fun sendAsRetry(batch: ByteBuffer): LokiResponse {
        val response = try {
            delegate.send(batch)
        } catch (e: Exception) {
            scheduleNextRetry(describe(e))
            return DROPPED
        }
        if (!isSuccess(response)) {
            scheduleNextRetry("HTTP ${response.status}")
            return DROPPED
        }
        consecutiveFailures.set(0)
        if (suspended.compareAndSet(true, false)) {
            log.info("Loki 로그 전송이 복구되었습니다. 전송을 재개합니다.")
        }
        return response
    }

    private fun countFailure(cause: String) {
        val failures = consecutiveFailures.incrementAndGet()
        if (failures < attemptLimit) {
            log.warn("Loki 로그 전송에 실패했습니다. ({}/{}) 원인: {}", failures, attemptLimit, cause)
            return
        }
        nextRetryAt.set(ticker() + retryInterval.toNanos())
        if (suspended.compareAndSet(false, true)) {
            log.warn(
                "Loki 로그 전송이 {}번 연속 실패했습니다. 전송을 중단하고 {} 후 1회 재시도합니다. 원인: {}",
                failures, retryInterval, cause,
            )
        }
    }

    private fun scheduleNextRetry(cause: String) {
        nextRetryAt.set(ticker() + retryInterval.toNanos())
        log.warn("Loki 로그 전송 재시도에 실패했습니다. {} 후 1회 더 시도합니다. 원인: {}", retryInterval, cause)
    }

    private fun describe(cause: Throwable?): String {
        if (cause == null) {
            return "알 수 없음"
        }
        // ConnectException 처럼 message 가 null 인 예외가 "null" 로 찍히지 않게 합니다.
        val name = cause::class.java.simpleName
        val message = cause.message ?: cause.cause?.message
        return if (message.isNullOrBlank()) name else "$name: $message"
    }
}
