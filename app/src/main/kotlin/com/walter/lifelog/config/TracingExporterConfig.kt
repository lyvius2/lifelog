package com.walter.lifelog.config

import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.SpanExporters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 자동 구성이 만드는 `SpanExporters` 빈을 대체해, 각 [SpanExporter] 를
 * [ResilientSpanExporter] 로 감싼다.
 *
 * `OpenTelemetryTracingAutoConfiguration.spanExporters` 가 `@ConditionalOnMissingBean`
 * 이므로 이 빈이 있으면 자동 구성은 물러난다. OTLP exporter 자체는 자동 구성이 그대로
 * 만들어 주므로 endpoint, timeout 등 기존 설정은 모두 유지된다.
 */
@Configuration
@EnableConfigurationProperties(TracingExporterProperties::class)
@ConditionalOnProperty(prefix = "management.tracing.export", name = ["enabled"], havingValue = "true")
class TracingExporterConfig {

    @Bean
    fun spanExporters(
        exporters: ObjectProvider<SpanExporter>,
        properties: TracingExporterProperties,
    ): SpanExporters {
        val resolved: List<SpanExporter> = exporters.orderedStream().toList()
        if (!properties.resilient) {
            return SpanExporters.of(resolved)
        }
        return SpanExporters.of(resolved.map { ResilientSpanExporter(it, properties.retryInterval) })
    }
}
