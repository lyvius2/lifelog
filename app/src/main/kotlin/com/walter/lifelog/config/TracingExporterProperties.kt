package com.walter.lifelog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Trace 전송 실패 시의 재시도 정책 설정.
 */
@ConfigurationProperties(prefix = "tracing.exporter")
data class TracingExporterProperties(
    /** 전송 실패 시 전송을 중단하고 주기적으로만 재시도할지 여부. false 면 OpenTelemetry 기본 동작. */
    val resilient: Boolean = true,
    /** 연속 실패를 몇 번까지 허용할지. 이 횟수에 도달하면 전송을 중단한다. 1 미만은 1 로 본다. */
    val maxAttempts: Int = 3,
    /** 중단 상태에서 재시도까지의 간격. 이 간격마다 1회만 재시도한다. */
    val retryInterval: Duration = Duration.ofHours(1),
)
