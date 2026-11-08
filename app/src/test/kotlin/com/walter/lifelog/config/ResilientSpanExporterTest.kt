package com.walter.lifelog.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("ResilientSpanExporter 테스트")
class ResilientSpanExporterTest {

    private val delegate: SpanExporter = mockk()
    private var now: Long = 0L
    private val retryInterval: Duration = Duration.ofHours(1)

    /** 중단까지의 동작을 검증하는 테스트는 임계값 1 로 단순화합니다. */
    private val sut = createExporter(maxAttempts = 1)

    @Test
    @DisplayName("export - 전송이 계속 성공하면 호출마다 위임한다")
    fun export_alwaysSuccess_delegatesEveryCall() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofSuccess()

        // when
        repeat(3) { sut.export(createSpans()) }

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 최초 전송이 실패하면 이후에는 전송을 시도하지 않는다")
    fun export_firstFailure_stopsExporting() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()

        // when
        val first = sut.export(createSpans())
        val second = sut.export(createSpans())
        val third = sut.export(createSpans())

        // then
        verify(exactly = 1) { delegate.export(any()) }
        assertThat(first.isSuccess).isFalse()
        assertThat(second.isSuccess).isTrue()
        assertThat(third.isSuccess).isTrue()
    }

    @Test
    @DisplayName("export - 재시도 간격이 지나기 전에는 재시도하지 않는다")
    fun export_beforeRetryInterval_doesNotRetry() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        sut.export(createSpans())

        // when
        now += retryInterval.toNanos() - 1
        sut.export(createSpans())

        // then
        verify(exactly = 1) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 재시도 간격이 지나면 1회만 재시도한다")
    fun export_afterRetryInterval_retriesOnlyOnce() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        sut.export(createSpans())

        // when
        now += retryInterval.toNanos()
        sut.export(createSpans())
        sut.export(createSpans())
        sut.export(createSpans())

        // then
        verify(exactly = 2) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 재시도가 성공하면 전송을 재개한다")
    fun export_retrySucceeds_resumesExporting() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        sut.export(createSpans())

        // when
        every { delegate.export(any()) } returns CompletableResultCode.ofSuccess()
        now += retryInterval.toNanos()
        sut.export(createSpans())
        sut.export(createSpans())

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 재시도가 실패하면 다시 재시도 간격만큼 기다린 뒤 1회 재시도한다")
    fun export_retryFails_waitsAnotherInterval() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        sut.export(createSpans())
        now += retryInterval.toNanos()
        sut.export(createSpans())

        // when
        sut.export(createSpans())
        now += retryInterval.toNanos()
        sut.export(createSpans())

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    @Test
    @DisplayName("flush - 중단 상태에서는 위임하지 않고 성공으로 응답한다")
    fun flush_whenSuspended_doesNotDelegate() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        sut.export(createSpans())

        // when
        val result = sut.flush()

        // then
        assertThat(result.isSuccess).isTrue()
        verify(exactly = 0) { delegate.flush() }
    }

    @Test
    @DisplayName("flush - 전송 중 상태에서는 위임한다")
    fun flush_whenExporting_delegates() {
        // given
        every { delegate.flush() } returns CompletableResultCode.ofSuccess()

        // when
        sut.flush()

        // then
        verify(exactly = 1) { delegate.flush() }
    }

    @Test
    @DisplayName("shutdown - 중단 상태에서도 자원 정리를 위해 위임한다")
    fun shutdown_whenSuspended_delegates() {
        // given
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        every { delegate.shutdown() } returns CompletableResultCode.ofSuccess()
        sut.export(createSpans())

        // when
        sut.shutdown()

        // then
        verify(exactly = 1) { delegate.shutdown() }
    }

    @Test
    @DisplayName("export - maxAttempts 가 3 이면 3번 연속 실패할 때까지는 계속 전송을 시도한다")
    fun export_maxAttemptsThree_keepsTryingUntilThirdFailure() {
        // given
        val exporter = createExporter(maxAttempts = 3)
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()

        // when
        repeat(5) { exporter.export(createSpans()) }

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 임계값에 도달하기 전에 성공하면 연속 실패 횟수가 초기화된다")
    fun export_successBeforeThreshold_resetsFailureCount() {
        // given
        val exporter = createExporter(maxAttempts = 3)

        // when
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        exporter.export(createSpans())
        exporter.export(createSpans())
        every { delegate.export(any()) } returns CompletableResultCode.ofSuccess()
        exporter.export(createSpans())
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        repeat(5) { exporter.export(createSpans()) }

        // then
        // 실패 2 + 성공 1 로 초기화된 뒤, 다시 3번 실패하고 중단합니다
        verify(exactly = 6) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 중단 후 재시도는 maxAttempts 와 무관하게 간격당 1회만 시도한다")
    fun export_afterSuspend_retriesOncePerIntervalRegardlessOfMaxAttempts() {
        // given
        val exporter = createExporter(maxAttempts = 3)
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()
        repeat(3) { exporter.export(createSpans()) }

        // when
        now += retryInterval.toNanos()
        repeat(4) { exporter.export(createSpans()) }

        // then
        verify(exactly = 4) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - maxAttempts 가 0 이하면 1 로 보아 첫 실패에 중단한다")
    fun export_maxAttemptsBelowOne_treatedAsOne() {
        // given
        val exporter = createExporter(maxAttempts = 0)
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()

        // when
        repeat(3) { exporter.export(createSpans()) }

        // then
        verify(exactly = 1) { delegate.export(any()) }
    }

    private fun createExporter(maxAttempts: Int): ResilientSpanExporter =
        ResilientSpanExporter(
            delegate = delegate,
            retryInterval = retryInterval,
            maxAttempts = maxAttempts,
            ticker = { now },
        )

    private fun createSpans(): List<SpanData> = listOf(mockk(relaxed = true))
}
