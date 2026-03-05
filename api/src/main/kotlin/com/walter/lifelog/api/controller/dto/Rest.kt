package com.walter.lifelog.api.controller.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공통 API 응답 형식")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Rest<T>(
    @Schema(description = "응답 시간", example = "2025-12-19T14:00:00")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "HTTP 상태 코드", example = "200")
    val statusCode: Int,

    @Schema(description = "성공 여부", example = "true")
    val success: Boolean = statusCode < 400,

    @Schema(description = "응답 메시지", example = "OK")
    val message: String,

    @Schema(description = "실제 응답 데이터")
    val data: T? = null
) {
    companion object {
        fun <T> ok(data: T) = Rest(LocalDateTime.now(), 200, true, "OK", data)
        fun ok() = Rest(LocalDateTime.now(), 200, true, "OK", null)
    }
}