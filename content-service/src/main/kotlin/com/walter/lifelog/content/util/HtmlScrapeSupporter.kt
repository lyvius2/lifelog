package com.walter.lifelog.content.util

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * 공개된 주소에서 HTML 마크업을 내려받습니다.
 *
 * 리다이렉트를 드라이버에 맡기지 않고 직접 따라갑니다. 자동 추적에 맡기면 최종 목적지가
 * 사설망이어도 검사를 거치지 않기 때문입니다. 홉마다 [validateTarget] 을 다시 거칩니다.
 */
@Component
class HtmlScrapeSupporter(
    private val htmlMarkupSupporter: HtmlMarkupSupporter,
) {
    companion object {
        private const val MAX_REDIRECTS = 3
        private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
        private val REQUEST_TIMEOUT = Duration.ofSeconds(15)
        private const val USER_AGENT = "lifelog-static-page-fetcher/1.0"
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private val HTML_CONTENT_TYPES = listOf("text/html", "application/xhtml+xml")
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    fun fetchMarkup(rawUrl: String): String {
        var target = parseUrl(rawUrl)
        repeat(MAX_REDIRECTS + 1) {
            validateTarget(target)
            val response = send(target)
            val location = redirectLocation(response)
            if (location == null) {
                validateResponse(response)
                return htmlMarkupSupporter.validateMarkup(readBody(response))
            }
            target = target.resolve(location)
        }
        throw IllegalArgumentException("리다이렉트가 너무 많습니다. 최종 주소를 직접 입력해주세요.")
    }

    private fun parseUrl(rawUrl: String): URI {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotBlank()) { "주소를 입력해주세요." }
        val uri = runCatching { URI(trimmed) }.getOrNull()
        requireNotNull(uri) { "주소 형식이 올바르지 않습니다." }
        return uri
    }

    /**
     * 사설망·루프백으로 향하는 요청을 막습니다. 이 앱은 MySQL·MongoDB·Valkey 와 같은
     * 호스트에서 돌기 때문에, 주소를 그대로 믿으면 내부 서비스를 대신 긁어올 수 있습니다.
     */
    private fun validateTarget(uri: URI) {
        require(uri.scheme?.lowercase() in ALLOWED_SCHEMES) { "http 또는 https 주소만 등록할 수 있습니다." }
        val host = uri.host
        require(!host.isNullOrBlank()) { "주소에 호스트가 없습니다." }

        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull()
        requireNotNull(addresses) { "주소를 찾을 수 없습니다: $host" }
        require(addresses.none { it.isInternal() }) { "내부망 주소는 등록할 수 없습니다: $host" }
    }

    private fun InetAddress.isInternal(): Boolean =
        isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress ||
            isAnyLocalAddress || isMulticastAddress

    private fun send(uri: URI): HttpResponse<java.io.InputStream> {
        val request = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        return runCatching { httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()) }
            .getOrElse { throw IllegalArgumentException("주소에 접속할 수 없습니다: ${it.message}") }
    }

    private fun redirectLocation(response: HttpResponse<*>): String? {
        if (response.statusCode() !in 300..399) return null
        return response.headers().firstValue("Location").orElse(null)
            ?: throw IllegalArgumentException("리다이렉트 응답에 Location 이 없습니다.")
    }

    private fun validateResponse(response: HttpResponse<*>) {
        require(response.statusCode() == 200) {
            "주소가 정상 응답하지 않습니다 (HTTP ${response.statusCode()})."
        }
        val contentType = response.headers().firstValue("Content-Type").orElse("").lowercase()
        require(contentType.isBlank() || HTML_CONTENT_TYPES.any { contentType.startsWith(it) }) {
            "HTML 문서가 아닙니다 (Content-Type: $contentType)."
        }
    }

    private fun readBody(response: HttpResponse<java.io.InputStream>): String {
        val limit = HtmlMarkupSupporter.MAX_BYTES
        val bytes = response.body().use { it.readNBytes(limit + 1) }
        require(bytes.size <= limit) { "마크업이 너무 큽니다. 최대 ${limit / 1024 / 1024}MB 까지 등록할 수 있습니다." }
        return String(bytes, resolveCharset(response))
    }

    private fun resolveCharset(response: HttpResponse<*>): Charset {
        val contentType = response.headers().firstValue("Content-Type").orElse("")
        val name = Regex("charset=\"?([^\";]+)", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1)?.trim()
            ?: return StandardCharsets.UTF_8
        return runCatching { Charset.forName(name) }.getOrDefault(StandardCharsets.UTF_8)
    }
}
