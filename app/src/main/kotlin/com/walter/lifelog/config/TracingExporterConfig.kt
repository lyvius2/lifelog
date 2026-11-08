package com.walter.lifelog.config

import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.SpanExporters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * 자동 구성의 `SpanExporters` 빈을 대체해 각 [SpanExporter] 를 [ResilientSpanExporter] 로
 * 감싼다. `OpenTelemetryTracingAutoConfiguration.spanExporters` 가
 * `@ConditionalOnMissingBean` 이므로 이 빈이 있으면 자동 구성은 물러난다.
 */
@Configuration
@EnableConfigurationProperties(TracingExporterProperties::class)
@ConditionalOnProperty(prefix = "management.tracing.export", name = ["enabled"], havingValue = "true")
class TracingExporterConfig {

    @Bean
    fun spanExporters(
        exporters: ObjectProvider<SpanExporter>,
        properties: TracingExporterProperties,
        environment: Environment,
    ): SpanExporters {
        val resolved: List<SpanExporter> = exporters.orderedStream().toList()
        if (!properties.resilient) {
            return SpanExporters.of(resolved)
        }
        val endpoint = environment.getProperty(OTLP_ENDPOINT_PROPERTY)
        return SpanExporters.of(
            resolved.map { exporter ->
                ResilientSpanExporter(
                    delegate = exporter,
                    suspension = ExportSuspensionHandler.forUrl(
                        subject = "Trace",
                        url = endpoint,
                        maxAttempts = properties.maxAttempts,
                        retryInterval = properties.retryInterval,
                        probeTimeout = properties.probeTimeout,
                    ),
                )
            },
        )
    }

    companion object {
        const val OTLP_ENDPOINT_PROPERTY = "management.opentelemetry.tracing.export.otlp.endpoint"
    }
}
