package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpConfig
import com.github.loki4j.client.http.Loki4jHttpClient
import com.github.loki4j.logback.JavaHttpSender
import java.time.Duration
import java.util.function.Function

/**
 * loki4j 의 기본 HTTP sender 에 [ResilientLoki4jHttpClient] 를 끼운다.
 * `logback-spring.xml` 의 `<http class="...">` 로 지정하며 속성을 XML 에서 받는다.
 */
class ResilientHttpSender : JavaHttpSender() {

    var maxAttempts: Int = 3

    var retryIntervalMs: Long = Duration.ofHours(1).toMillis()

    var probeTimeoutMs: Long = 2_000

    var resilient: Boolean = true

    override fun getHttpClientFactory(): Function<HttpConfig, Loki4jHttpClient> {
        val base = super.getHttpClientFactory()
        if (!resilient) {
            return base
        }
        return Function { config ->
            ResilientLoki4jHttpClient(
                delegate = base.apply(config),
                suspension = ExportSuspensionHandler.forUrl(
                    subject = "Loki 로그",
                    url = config.pushUrl,
                    maxAttempts = maxAttempts,
                    retryInterval = Duration.ofMillis(retryIntervalMs),
                    probeTimeout = Duration.ofMillis(probeTimeoutMs),
                ),
            )
        }
    }
}
