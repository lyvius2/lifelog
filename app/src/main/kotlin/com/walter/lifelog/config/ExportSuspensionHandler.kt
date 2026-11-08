package com.walter.lifelog.config

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 관측 데이터 전송의 중단·재개를 관리한다. 연속 [maxAttempts] 회 실패하면 중단하고,
 * 전용 스케줄러가 [retryInterval] 간격으로 [probe] 를 돌려 연결이 확인되면 재개한다.
 * trace([ResilientSpanExporter]) 와 로그([ResilientLoki4jHttpClient]) 가 공유한다.
 *
 * [scheduler] 는 [close] 시점에 종료하므로 외부와 공유해서는 안 된다.
 */
class ExportSuspensionHandler(
    private val subject: String,
    maxAttempts: Int,
    private val retryInterval: Duration,
    private val probe: () -> Boolean,
    private val scheduler: ScheduledExecutorService = newProbeScheduler(subject),
) : AutoCloseable {

    private val attemptLimit = maxAttempts.coerceAtLeast(1)

    private val consecutiveFailures = AtomicInteger(0)
    private val suspended = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val probeTask = AtomicReference<ScheduledFuture<*>?>(null)

    val isSuspended: Boolean
        get() = suspended.get()

    fun onSuccess() {
        consecutiveFailures.set(0)
    }

    fun onFailure(cause: Throwable?) = onFailure(describe(cause))

    fun onFailure(cause: String) {
        val failures = consecutiveFailures.incrementAndGet()
        if (failures < attemptLimit) {
            log.warn("{} 전송에 실패했습니다. ({}/{}) 원인: {}", subject, failures, attemptLimit, cause)
            return
        }
        if (!suspended.compareAndSet(false, true)) {
            return
        }
        log.warn(
            "{} 전송이 {}번 연속 실패했습니다. 전송을 중단하고 {} 간격으로 연결을 확인합니다. 원인: {}",
            subject, failures, retryInterval, cause,
        )
        startProbing()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        stopProbing()
        scheduler.shutdownNow()
    }

    private fun startProbing() {
        if (closed.get()) {
            return
        }
        val intervalMs = retryInterval.toMillis().coerceAtLeast(1)
        val task = try {
            scheduler.scheduleWithFixedDelay({ probeOnce() }, intervalMs, intervalMs, TimeUnit.MILLISECONDS)
        } catch (e: RejectedExecutionException) {
            log.warn("{} 연결 확인을 예약할 수 없습니다. 전송이 중단된 상태로 남습니다.", subject, e)
            return
        }
        probeTask.set(task)
        if (closed.get() || !suspended.get()) {
            stopProbing()
        }
    }

    private fun probeOnce() {
        if (!suspended.get()) {
            return
        }
        if (!runCatching { probe() }.getOrDefault(false)) {
            log.warn("{} 수집기에 연결할 수 없습니다. {} 후 다시 확인합니다.", subject, retryInterval)
            return
        }
        if (suspended.compareAndSet(true, false)) {
            consecutiveFailures.set(0)
            log.info("{} 수집기 연결이 확인되었습니다. 전송을 재개합니다.", subject)
        }
        stopProbing()
    }

    private fun stopProbing() {
        probeTask.getAndSet(null)?.cancel(false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExportSuspensionHandler::class.java)

        private val DEFAULT_PROBE_TIMEOUT: Duration = Duration.ofSeconds(2)

        /** [url] 을 해석할 수 없으면 간격마다 전송을 그냥 재개해 본다(실패하면 다시 중단된다). */
        fun forUrl(
            subject: String,
            url: String?,
            maxAttempts: Int,
            retryInterval: Duration,
            probeTimeout: Duration = DEFAULT_PROBE_TIMEOUT,
        ): ExportSuspensionHandler {
            val probe = TcpConnectionProbe.of(url, probeTimeout)
            if (probe == null) {
                log.warn(
                    "{} 수집기 주소를 해석할 수 없습니다(url={}). 연결 확인 없이 {} 마다 전송을 재개해 봅니다.",
                    subject, url, retryInterval,
                )
            }
            return ExportSuspensionHandler(
                subject = subject,
                maxAttempts = maxAttempts,
                retryInterval = retryInterval,
                probe = probe ?: { true },
            )
        }

        private fun newProbeScheduler(subject: String): ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "observability-probe($subject)").apply { isDaemon = true }
            }

        private fun describe(cause: Throwable?): String {
            if (cause == null) {
                return "알 수 없음"
            }
            val name = cause::class.java.simpleName
            val message = cause.message ?: cause.cause?.message
            return if (message.isNullOrBlank()) name else "$name: $message"
        }
    }
}
