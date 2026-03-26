package com.walter.lifelog.api.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI 요약 생성 요청")
data class CreateSummaryRequest(
    @Schema(description = "요약할 게시글 본문 (마크다운)", requiredMode = Schema.RequiredMode.REQUIRED, example = "# Spring Boot 4의 변경점\n\nSpring Boot 4에서는...")
    val content: String,
)
