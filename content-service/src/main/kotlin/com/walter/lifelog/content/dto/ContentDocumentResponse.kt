package com.walter.lifelog.content.dto

import com.walter.lifelog.content.entity.ContentDocuments
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "콘텐츠 문서 조회 응답 DTO")
data class ContentDocumentResponse(
    @Schema(description = "문서 ID (MongoDB ObjectId)", example = "665f1a2b3c4d5e6f7a8b9c0d")
    val id: String?,

    @Schema(description = "ContentType 열거값 이름", example = "INTRO")
    val contentType: String,

    @Schema(description = "ContentType 설명", example = "웹사이트 개요")
    val typeDescription: String,

    @Schema(description = "콘텐츠 본문 (JSON 구조)")
    val content: HashMap<String, Any>,

    @Schema(description = "최종 수정일시", example = "2026-03-30T12:00:00")
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(document: ContentDocuments) = ContentDocumentResponse(
            id = document.id,
            contentType = document.contentType.name,
            typeDescription = document.contentType.typeDescription,
            content = document.content,
            updatedAt = document.updatedAt,
        )
    }
}
