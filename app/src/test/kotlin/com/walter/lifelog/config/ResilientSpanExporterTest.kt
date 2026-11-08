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
import java.util.concurrent.ScheduledExecutorService

@DisplayName("ResilientSpanExporter 테스트")
class ResilientSpanExporterTest {

    private val delegate: SpanExporter = mockk()
    private val suspension: ExportSuspensionHandler = mockk(relaxed = true)
    private val sut = ResilientSpanExporter(delegate = delegate, suspension = suspension)

    @Test
    @DisplayName("export - 전송 중 상태에서는 호출마다 위임한다")
    fun export_whenExporting_delegatesEveryCall() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.export(any()) } returns CompletableResultCode.ofSuccess()

        // when
        repeat(3) { sut.export(createSpans()) }

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    @Test
    @DisplayName("export - 전송이 성공하면 핸들러에 성공을 알린다")
    fun export_success_notifiesHandler() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.export(any()) } returns CompletableResultCode.ofSuccess()

        // when
        sut.export(createSpans())

        // then
        verify(exactly = 1) { suspension.onSuccess() }
        verify(exactly = 0) { suspension.onFailure(any<Throwable>()) }
    }

    @Test
    @DisplayName("export - 전송이 실패하면 핸들러에 실패를 알리고 결과를 그대로 돌려준다")
    fun export_failure_notifiesHandler() {
        // given
        every { suspension.isSuspended } returns false
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()

        // when
        val result = sut.export(createSpans())

        // then
        assertThat(result.isSuccess).isFalse()
        verify(exactly = 1) { suspension.onFailure(any<Throwable>()) }
    }

    @Test
    @DisplayName("export - 중단 상태에서는 위임하지 않고 성공으로 응답한다")
    fun export_whenSuspended_doesNotDelegate() {
        // given
        every { suspension.isSuspended } returns true

        // when
        val result = sut.export(createSpans())

        // then
        assertThat(result.isSuccess).isTrue()
        verify(exactly = 0) { delegate.export(any()) }
    }

    @Test
    @DisplayName("flush - 중단 상태에서는 위임하지 않고 성공으로 응답한다")
    fun flush_whenSuspended_doesNotDelegate() {
        // given
        every { suspension.isSuspended } returns true

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
        every { suspension.isSuspended } returns false
        every { delegate.flush() } returns CompletableResultCode.ofSuccess()

        // when
        sut.flush()

        // then
        verify(exactly = 1) { delegate.flush() }
    }

    @Test
    @DisplayName("shutdown - 연결 확인 스케줄러를 정리하고 위임한다")
    fun shutdown_closesHandlerAndDelegates() {
        // given
        every { delegate.shutdown() } returns CompletableResultCode.ofSuccess()

        // when
        sut.shutdown()

        // then
        verify(exactly = 1) { suspension.close() }
        verify(exactly = 1) { delegate.shutdown() }
    }

    @Test
    @DisplayName("export - 연속 실패가 maxAttempts 에 도달하면 이후 전송을 건너뛴다")
    fun export_reachesMaxAttempts_stopsDelegating() {
        // given
        val scheduler: ScheduledExecutorService = mockk(relaxed = true)
        val exporter = ResilientSpanExporter(
            delegate = delegate,
            suspension = ExportSuspensionHandler(
                subject = "Trace",
                maxAttempts = 3,
                retryInterval = Duration.ofHours(1),
                probe = { false },
                scheduler = scheduler,
            ),
        )
        every { delegate.export(any()) } returns CompletableResultCode.ofFailure()

        // when
        repeat(10) { exporter.export(createSpans()) }

        // then
        verify(exactly = 3) { delegate.export(any()) }
    }

    private fun createSpans(): List<SpanData> = listOf(mockk(relaxed = true))
}
