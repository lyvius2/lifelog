package com.walter.lifelog.content.dto

import com.walter.lifelog.content.entity.code.ContentType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ContentType 정보 DTO")
data class ContentTypeInfo(
    @Schema(description = "ContentType 열거값 이름", example = "INTRO")
    val contentType: String,

    @Schema(description = "ContentType 설명", example = "웹사이트 개요")
    val typeDescription: String,

    @Schema(description = "저장된 문서 존재 여부", example = "true")
    val hasDocument: Boolean,
) {
    companion object {
        fun of(contentType: ContentType, hasDocument: Boolean) = ContentTypeInfo(
            contentType = contentType.name,
            typeDescription = contentType.typeDescription,
            hasDocument = hasDocument,
        )
    }
}
