package com.walter.lifelog.content.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "콘텐츠 문서 저장 요청 DTO")
data class ContentDocumentRequest(
    @Schema(description = "ContentType 열거값 이름", example = "INTRO", requiredMode = Schema.RequiredMode.REQUIRED)
    val contentType: String,

    @Schema(description = "콘텐츠 본문 (JSON 구조)", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: HashMap<String, Any>,
)