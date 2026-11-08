package com.walter.lifelog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "tracing.exporter")
data class TracingExporterProperties(
    val resilient: Boolean = true,
    val maxAttempts: Int = 3,
    val retryInterval: Duration = Duration.ofHours(1),
    val probeTimeout: Duration = Duration.ofSeconds(2),
)
