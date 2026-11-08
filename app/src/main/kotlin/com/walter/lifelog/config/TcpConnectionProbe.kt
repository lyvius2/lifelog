package com.walter.lifelog.config

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.time.Duration

/**
 * 수집기 주소에 TCP 연결이 되는지만 확인한다.
 *
 * 빈 배치를 보내 보는 방식은 쓰지 않는다. OTLP exporter 는 span 이 없으면 HTTP 요청
 * 없이 성공으로 응답할 수 있어 수집기가 없는데도 복구로 오판할 수 있다.
 */
class TcpConnectionProbe(
    private val host: String,
    private val port: Int,
    private val timeout: Duration,
) : () -> Boolean {

    override fun invoke(): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout.toMillis().toInt())
                true
            }
        } catch (e: Exception) {
            false
        }

    override fun toString(): String = "$host:$port"

    companion object {
        fun of(url: String?, timeout: Duration): TcpConnectionProbe? {
            if (url.isNullOrBlank()) {
                return null
            }
            val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
            val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
            val port = if (uri.port > 0) uri.port else defaultPortOf(uri.scheme) ?: return null
            return TcpConnectionProbe(host, port, timeout)
        }

        private fun defaultPortOf(scheme: String?): Int? =
            when (scheme?.lowercase()) {
                "http" -> 80
                "https" -> 443
                else -> null
            }
    }
}
