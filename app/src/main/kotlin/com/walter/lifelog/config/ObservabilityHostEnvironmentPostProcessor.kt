package com.walter.lifelog.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.net.URI

/**
 * Loki 주소에서 호스트를 뽑아 [HOST_PROPERTY] 로 노출한다.
 *
 * Trace 와 로그는 같은 관측 스택(Tempo + Loki)으로 보내는 것이 보통이므로, 호스트를
 * 두 번 지정하게 하지 않는다. `LOKI_URL` 하나만 주면 trace 엔드포인트의 기본 호스트가
 * 따라온다.
 *
 * ```
 * LOKI_URL=http://observe.example:3100/loki/api/v1/push
 *   -> lifelog.observability.host = observe.example
 *   -> trace 엔드포인트 기본값 http://observe.example:4318/v1/traces
 * ```
 *
 * `LOKI_URL` 이 없거나 호스트를 읽을 수 없으면 [DEFAULT_HOST] 를 쓴다. 엔드포인트를
 * 통째로 다르게 주려면 `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` 를
 * 지정하면 된다 — 환경변수가 yml 의 기본값보다 우선한다.
 *
 * 속성은 [org.springframework.core.env.MutablePropertySources.addLast] 로 가장 낮은
 * 우선순위에 추가하므로, 같은 이름을 직접 지정하면 그 값이 이긴다.
 */
class ObservabilityHostEnvironmentPostProcessor : EnvironmentPostProcessor {

    companion object {
        /** 이 후처리기가 채우는 속성 이름. yml 의 trace 엔드포인트가 참조한다. */
        const val HOST_PROPERTY = "lifelog.observability.host"

        /** 호스트를 뽑아낼 원본. logback-spring.xml 의 Loki appender 와 같은 변수다. */
        const val LOKI_URL_PROPERTY = "LOKI_URL"

        const val DEFAULT_HOST = "localhost"

        private const val SOURCE_NAME = "lifelogObservabilityHost"
    }

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        if (environment.propertySources.contains(SOURCE_NAME)) {
            return
        }
        val host = hostOf(environment.getProperty(LOKI_URL_PROPERTY))
        environment.propertySources.addLast(
            MapPropertySource(SOURCE_NAME, mapOf(HOST_PROPERTY to host)),
        )
    }

    private fun hostOf(lokiUrl: String?): String {
        if (lokiUrl.isNullOrBlank()) {
            return DEFAULT_HOST
        }
        // 잘못된 주소 때문에 기동이 실패하면 안 됩니다. 읽히지 않으면 기본값으로 돌아갑니다.
        return runCatching { URI(lokiUrl.trim()).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_HOST
    }
}
