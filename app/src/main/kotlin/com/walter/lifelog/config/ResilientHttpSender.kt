package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpConfig
import com.github.loki4j.client.http.Loki4jHttpClient
import com.github.loki4j.logback.JavaHttpSender
import java.time.Duration
import java.util.function.Function

/**
 * loki4j 의 기본 HTTP sender 에 [ResilientLoki4jHttpClient] 를 끼우는 sender.
 *
 * `logback-spring.xml` 의 `<http class="...">` 로 지정하며, 아래 속성을 XML 에서 받는다.
 *
 * ```xml
 * <http class="com.walter.lifelog.config.ResilientHttpSender">
 *     <url>...</url>
 *     <maxAttempts>3</maxAttempts>
 *     <retryIntervalMs>3600000</retryIntervalMs>
 * </http>
 * ```
 *
 * 재시도 정책을 이 래퍼가 단독으로 갖도록 appender 쪽 `maxRetries` 는 0 으로 둔다.
 * loki4j 가 배치마다 자체 재시도(`Retry #1`, `Retry #2`)를 하면 한 배치가 실패할 때
 * 시도 횟수가 한꺼번에 소진되고, 중단 후 재시도 1회도 내부 재시도와 뒤섞인다.
 */
class ResilientHttpSender : JavaHttpSender() {

    /** 연속 이 횟수만큼 실패하면 전송을 중단한다. */
    var maxAttempts: Int = 3

    /** 중단 상태에서 재시도까지의 간격(밀리초). 기본 1시간. */
    var retryIntervalMs: Long = Duration.ofHours(1).toMillis()

    /** false 면 감싸지 않고 loki4j 기본 동작을 쓴다. */
    var resilient: Boolean = true

    override fun getHttpClientFactory(): Function<HttpConfig, Loki4jHttpClient> {
        val base = super.getHttpClientFactory()
        if (!resilient) {
            return base
        }
        return Function { config ->
            ResilientLoki4jHttpClient(
                delegate = base.apply(config),
                retryInterval = Duration.ofMillis(retryIntervalMs),
                maxAttempts = maxAttempts,
            )
        }
    }
}
