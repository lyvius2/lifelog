package com.walter.lifelog.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.net.URI

/**
 * `LOKI_URL` 에서 호스트를 뽑아 [HOST_PROPERTY] 로 노출한다. trace 엔드포인트의 기본
 * 호스트가 이 값을 참조하므로 관측 스택 주소를 한 번만 지정하면 된다.
 *
 * `addLast` 로 가장 낮은 우선순위에 추가하므로 같은 이름을 직접 주면 그 값이 이긴다.
 */
class ObservabilityHostEnvironmentPostProcessor : EnvironmentPostProcessor {

    companion object {
        const val HOST_PROPERTY = "lifelog.observability.host"
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

    /** 잘못된 주소 때문에 기동이 실패하면 안 되므로 읽히지 않으면 기본값으로 돌아간다. */
    private fun hostOf(lokiUrl: String?): String {
        if (lokiUrl.isNullOrBlank()) {
            return DEFAULT_HOST
        }
        return runCatching { URI(lokiUrl.trim()).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_HOST
    }
}
