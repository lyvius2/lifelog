package com.walter.lifelog.config

import com.github.loki4j.client.http.HttpConfig
import com.github.loki4j.client.http.HttpStatus
import com.github.loki4j.client.http.Loki4jHttpClient
import com.github.loki4j.client.http.LokiResponse
import java.nio.ByteBuffer

/**
 * 중단 상태에서는 배치를 버리는 [Loki4jHttpClient] 데코레이터. 정책은 [suspension] 이 맡는다.
 * 중단 전의 실패는 loki4j 가 평소처럼 로그를 남기도록 그대로 전파하고, 중단 후에는
 * Loki 의 정상 응답과 같은 204 로 응답해 오류가 더 쌓이지 않게 한다.
 */
class ResilientLoki4jHttpClient(
    private val delegate: Loki4jHttpClient,
    private val suspension: ExportSuspensionHandler,
) : Loki4jHttpClient {

    override fun getConfig(): HttpConfig = delegate.config

    override fun send(batch: ByteBuffer): LokiResponse {
        if (suspension.isSuspended) {
            return DROPPED
        }
        val response = try {
            delegate.send(batch)
        } catch (e: Exception) {
            suspension.onFailure(e)
            throw e
        }
        if (isSuccess(response)) {
            suspension.onSuccess()
        } else {
            suspension.onFailure("HTTP ${response.status}")
        }
        return response
    }

    override fun close() {
        suspension.close()
        delegate.close()
    }

    companion object {
        private val DROPPED = LokiResponse(HttpStatus.NO_CONTENT, "")

        private fun isSuccess(response: LokiResponse): Boolean = response.status in 200..299
    }
}
