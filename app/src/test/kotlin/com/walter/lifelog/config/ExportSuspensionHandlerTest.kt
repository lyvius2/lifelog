package com.walter.lifelog.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@DisplayName("ExportSuspensionHandler 테스트")
class ExportSuspensionHandlerTest {

    private val scheduler: ScheduledExecutorService = mockk(relaxed = true)
    private val scheduledTask: ScheduledFuture<*> = mockk(relaxed = true)
    private val probeTask = slot<Runnable>()
    private val retryInterval: Duration = Duration.ofHours(1)

    private var probeResult = false
    private var probeCalls = 0

    init {
        every {
            scheduler.scheduleWithFixedDelay(capture(probeTask), any(), any(), any())
        } returns scheduledTask
    }

    @Test
    @DisplayName("onFailure - 임계값에 도달하기 전에는 중단하지 않고 연결 확인도 예약하지 않는다")
    fun onFailure_belowThreshold_doesNotSuspend() {
        // given
        val sut = createHandler(maxAttempts = 3)

        // when
        sut.onFailure("connection refused")
        sut.onFailure("connection refused")

        // then
        assertThat(sut.isSuspended).isFalse()
        verify(exactly = 0) { scheduler.scheduleWithFixedDelay(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("onFailure - 임계값에 도달하면 중단하고 재시도 간격으로 연결 확인을 예약한다")
    fun onFailure_reachesThreshold_suspendsAndSchedulesProbe() {
        // given
        val sut = createHandler(maxAttempts = 3)

        // when
        repeat(3) { sut.onFailure("connection refused") }

        // then
        assertThat(sut.isSuspended).isTrue()
        verify(exactly = 1) {
            scheduler.scheduleWithFixedDelay(any(), 3_600_000L, 3_600_000L, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    @DisplayName("onFailure - 임계값에 도달한 뒤 더 실패해도 연결 확인을 다시 예약하지 않는다")
    fun onFailure_afterSuspend_doesNotRescheduleProbe() {
        // given
        val sut = createHandler(maxAttempts = 1)

        // when
        repeat(5) { sut.onFailure("connection refused") }

        // then
        verify(exactly = 1) { scheduler.scheduleWithFixedDelay(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("onSuccess - 임계값에 도달하기 전에 성공하면 연속 실패 횟수가 초기화된다")
    fun onSuccess_beforeThreshold_resetsFailureCount() {
        // given
        val sut = createHandler(maxAttempts = 3)
        sut.onFailure("connection refused")
        sut.onFailure("connection refused")

        // when
        sut.onSuccess()
        sut.onFailure("connection refused")
        sut.onFailure("connection refused")

        // then
        assertThat(sut.isSuspended).isFalse()
    }

    @Test
    @DisplayName("onFailure - maxAttempts 가 0 이하면 1 로 보아 첫 실패에 중단한다")
    fun onFailure_maxAttemptsBelowOne_treatedAsOne() {
        // given
        val sut = createHandler(maxAttempts = 0)

        // when
        sut.onFailure("connection refused")

        // then
        assertThat(sut.isSuspended).isTrue()
    }

    @Test
    @DisplayName("연결 확인 - 트래픽이 없어도 예약된 탐침이 실행되어 연결을 시도한다")
    fun probe_runsWithoutTraffic() {
        // given
        val sut = createHandler(maxAttempts = 1)
        sut.onFailure("connection refused")
        probeResult = false

        // when
        repeat(3) { probeTask.captured.run() }

        // then
        assertThat(probeCalls).isEqualTo(3)
        assertThat(sut.isSuspended).isTrue()
        verify(exactly = 0) { scheduledTask.cancel(any()) }
    }

    @Test
    @DisplayName("연결 확인 - 연결이 확인되면 전송을 재개하고 예약을 해제한다")
    fun probe_whenReachable_resumesAndCancelsSchedule() {
        // given
        val sut = createHandler(maxAttempts = 1)
        sut.onFailure("connection refused")
        probeResult = true

        // when
        probeTask.captured.run()

        // then
        assertThat(sut.isSuspended).isFalse()
        verify(exactly = 1) { scheduledTask.cancel(false) }
    }

    @Test
    @DisplayName("연결 확인 - 재개 후 다시 실패가 쌓이면 중단하고 연결 확인을 다시 예약한다")
    fun probe_afterResume_suspendsAgainOnNewFailures() {
        // given
        val sut = createHandler(maxAttempts = 1)
        sut.onFailure("connection refused")
        probeResult = true
        probeTask.captured.run()

        // when
        sut.onFailure("connection refused")

        // then
        assertThat(sut.isSuspended).isTrue()
        verify(exactly = 2) { scheduler.scheduleWithFixedDelay(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("연결 확인 - 탐침이 예외를 던지면 닿지 않은 것으로 보고 중단을 유지한다")
    fun probe_throwing_keepsSuspended() {
        // given
        val sut = ExportSuspensionHandler(
            subject = "Trace",
            maxAttempts = 1,
            retryInterval = retryInterval,
            probe = { throw IllegalStateException("boom") },
            scheduler = scheduler,
        )
        sut.onFailure("connection refused")

        // when
        probeTask.captured.run()

        // then
        assertThat(sut.isSuspended).isTrue()
    }

    @Test
    @DisplayName("close - 예약을 해제하고 스케줄러를 종료한다")
    fun close_cancelsScheduleAndShutsDownScheduler() {
        // given
        val sut = createHandler(maxAttempts = 1)
        sut.onFailure("connection refused")

        // when
        sut.close()

        // then
        verify(exactly = 1) { scheduledTask.cancel(false) }
        verify(exactly = 1) { scheduler.shutdownNow() }
    }

    @Test
    @DisplayName("close - 두 번 호출해도 스케줄러를 한 번만 종료한다")
    fun close_calledTwice_shutsDownOnce() {
        // given
        val sut = createHandler(maxAttempts = 1)

        // when
        sut.close()
        sut.close()

        // then
        verify(exactly = 1) { scheduler.shutdownNow() }
    }

    @Test
    @DisplayName("close - 닫힌 뒤에 실패가 들어와도 연결 확인을 예약하지 않는다")
    fun close_thenFailure_doesNotSchedule() {
        // given
        val sut = createHandler(maxAttempts = 1)
        sut.close()

        // when
        sut.onFailure("connection refused")

        // then
        verify(exactly = 0) { scheduler.scheduleWithFixedDelay(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("forUrl - 주소를 해석할 수 없으면 간격마다 전송을 재개해 보는 핸들러를 만든다")
    fun forUrl_unparsableUrl_resumesOptimistically() {
        // given
        val sut = ExportSuspensionHandler.forUrl(
            subject = "Trace",
            url = "해석할 수 없는 주소",
            maxAttempts = 1,
            retryInterval = retryInterval,
        )

        // when
        sut.onFailure("connection refused")

        // then
        // 중단은 되지만, 연결 확인이 늘 성공으로 간주되므로 다음 간격에 전송을 재개합니다
        assertThat(sut.isSuspended).isTrue()
        sut.close()
    }

    private fun createHandler(maxAttempts: Int): ExportSuspensionHandler =
        ExportSuspensionHandler(
            subject = "Trace",
            maxAttempts = maxAttempts,
            retryInterval = retryInterval,
            probe = { probeCalls++; probeResult },
            scheduler = scheduler,
        )
}
