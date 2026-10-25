package com.walter.lifelog.config

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.SpanExporters
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@DisplayName("TracingExporterConfig 테스트")
class TracingExporterConfigTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                OpenTelemetrySdkAutoConfiguration::class.java,
                OpenTelemetryTracingAutoConfiguration::class.java,
                OtlpTracingAutoConfiguration::class.java,
            ),
        )
        .withUserConfiguration(TracingExporterConfig::class.java)

    @Test
    @DisplayName("spanExporters - 전송이 켜져 있으면 OTLP exporter 를 ResilientSpanExporter 로 감싼다")
    fun spanExporters_exportEnabled_wrapsOtlpExporter() {
        // given
        val context = runner.withPropertyValues(*createTracingEnabledProperties())

        // when / then
        context.run { ctx ->
            assertThat(ctx).hasSingleBean(SpanExporters::class.java)
            assertThat(ctx).hasSingleBean(OtlpHttpSpanExporter::class.java)

            val exporters = ctx.getBean(SpanExporters::class.java).list()
            assertThat(exporters).hasSize(1)
            assertThat(exporters.first()).isInstanceOf(ResilientSpanExporter::class.java)
        }
    }

    @Test
    @DisplayName("spanExporters - resilient 가 false 면 감싸지 않고 OTLP exporter 를 그대로 사용한다")
    fun spanExporters_resilientDisabled_doesNotWrap() {
        // given
        val context = runner.withPropertyValues(
            *createTracingEnabledProperties(),
            "tracing.exporter.resilient=false",
        )

        // when / then
        context.run { ctx ->
            val exporters = ctx.getBean(SpanExporters::class.java).list()
            assertThat(exporters).hasSize(1)
            assertThat(exporters.first()).isInstanceOf(OtlpHttpSpanExporter::class.java)
        }
    }

    @Test
    @DisplayName("spanExporters - 전송이 꺼져 있으면 이 설정이 적용되지 않는다")
    fun spanExporters_exportDisabled_doesNotApply() {
        // given
        val context = runner.withPropertyValues("management.tracing.export.enabled=false")

        // when / then
        context.run { ctx ->
            assertThat(ctx).doesNotHaveBean(TracingExporterConfig::class.java)
        }
    }

    @Test
    @DisplayName("retryInterval - 설정하지 않으면 1시간이 기본값이다")
    fun retryInterval_notConfigured_defaultsToOneHour() {
        // given
        val context = runner.withPropertyValues(*createTracingEnabledProperties())

        // when / then
        context.run { ctx ->
            val properties = ctx.getBean(TracingExporterProperties::class.java)
            assertThat(properties.resilient).isTrue()
            assertThat(properties.retryInterval).hasHours(1)
        }
    }

    private fun createTracingEnabledProperties(): Array<String> = arrayOf(
        "management.tracing.export.enabled=true",
        "management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces",
    )
}
