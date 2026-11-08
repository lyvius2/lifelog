package com.walter.lifelog.config

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * 중단 상태에서는 span 을 버리는 [SpanExporter] 데코레이터. 정책은 [suspension] 이 맡는다.
 * 버릴 때 성공으로 응답해 `BatchSpanProcessor` 가 실패 로그를 쌓지 않게 한다.
 */
class ResilientSpanExporter(
    private val delegate: SpanExporter,
    private val suspension: ExportSuspensionHandler,
) : SpanExporter {

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        if (suspension.isSuspended) {
            return CompletableResultCode.ofSuccess()
        }
        return delegate.export(spans).also { watch(it) }
    }

    override fun flush(): CompletableResultCode {
        if (suspension.isSuspended) {
            return CompletableResultCode.ofSuccess()
        }
        return delegate.flush()
    }

    override fun shutdown(): CompletableResultCode {
        suspension.close()
        return delegate.shutdown()
    }

    private fun watch(result: CompletableResultCode) {
        result.whenComplete {
            if (result.isSuccess) {
                suspension.onSuccess()
            } else {
                suspension.onFailure(result.failureThrowable)
            }
        }
    }
}
