package com.walter.lifelog.config

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Trace 전송이 실패하면 전송을 중단하고 [retryInterval] 마다 1회만 재시도하는 [SpanExporter] 데코레이터.
 *
 * Collector(Tempo)가 떠 있지 않을 때 BatchSpanProcessor 가 배치마다 전송을 시도하며
 * 로그를 쏟아내는 것을 막는다. 상태는 두 가지다.
 *
 * - **전송 중**: 평소처럼 위임한다. 실패하면 warn 로그를 1회 남기고 중단 상태로 넘어간다.
 * - **중단**: 전송을 시도하지 않는다. [retryInterval] 이 지나면 1회만 위임해 보고,
 *   성공하면 전송 중으로 복귀하며, 실패하면 warn 로그를 남긴 뒤 다시 [retryInterval] 을 기다린다.
 *
 * 중단 상태에서 넘어온 span 은 전송하지 않고 버리며, BatchSpanProcessor 가 내부적으로
 * 실패 로그를 남기지 않도록 성공으로 응답한다.
 */
class ResilientSpanExporter(
    private val delegate: SpanExporter,
    private val retryInterval: Duration,
    private val ticker: () -> Long = System::nanoTime,
) : SpanExporter {

    companion object {
        private val log = LoggerFactory.getLogger(ResilientSpanExporter::class.java)
    }

    private val suspended = AtomicBoolean(false)
    private val retrying = AtomicBoolean(false)
    private val nextRetryAt = AtomicLong(0)

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        if (!suspended.get()) {
            return delegate.export(spans).also { watch(it, retry = false) }
        }
        if (ticker() - nextRetryAt.get() < 0) {
            return CompletableResultCode.ofSuccess()
        }
        if (!retrying.compareAndSet(false, true)) {
            return CompletableResultCode.ofSuccess()
        }
        return delegate.export(spans).also { watch(it, retry = true) }
    }

    override fun flush(): CompletableResultCode {
        if (suspended.get()) {
            return CompletableResultCode.ofSuccess()
        }
        return delegate.flush()
    }

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    private fun watch(result: CompletableResultCode, retry: Boolean) {
        result.whenComplete {
            if (result.isSuccess) {
                onSuccess(retry)
            } else {
                onFailure(retry, result.failureThrowable)
            }
            if (retry) {
                retrying.set(false)
            }
        }
    }

    private fun onSuccess(retry: Boolean) {
        if (retry && suspended.compareAndSet(true, false)) {
            log.info("Trace 전송이 복구되었습니다. 전송을 재개합니다.")
        }
    }

    private fun onFailure(retry: Boolean, cause: Throwable?) {
        nextRetryAt.set(ticker() + retryInterval.toNanos())
        if (retry) {
            log.warn("Trace 전송 재시도에 실패했습니다. {} 후 1회 더 시도합니다. 원인: {}", retryInterval, describe(cause))
            return
        }
        if (suspended.compareAndSet(false, true)) {
            log.warn("Trace 전송에 실패했습니다. 전송을 중단하고 {} 후 1회 재시도합니다. 원인: {}", retryInterval, describe(cause))
        }
    }

    private fun describe(cause: Throwable?): String =
        cause?.let { "${it::class.java.simpleName}: ${it.message}" } ?: "알 수 없음"
}
