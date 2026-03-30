package com.walter.lifelog.api.controller

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.content.dto.ContentDocumentRequest
import com.walter.lifelog.content.dto.ContentDocumentResponse
import com.walter.lifelog.content.dto.ContentTypeInfo
import com.walter.lifelog.content.entity.code.ContentType
import com.walter.lifelog.content.service.ContentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "콘텐츠 문서", description = "MongoDB content-documents 컬렉션 관리 API")
@RequestMapping("/api/content")
@RestController
class ContentDocumentController(
    private val contentService: ContentService,
) {
    @Operation(summary = "ContentType 목록 조회", description = "등록된 모든 ContentType과 문서 존재 여부를 조회한다.")
    @GetMapping("/types")
    fun getContentTypes(): Rest<List<ContentTypeInfo>> {
        return Rest.ok(contentService.getAllTypeInfos())
    }

    @Operation(summary = "콘텐츠 문서 조회", description = "ContentType으로 콘텐츠 문서를 조회한다. 문서가 없으면 빈 content를 반환한다.")
    @GetMapping
    fun getContentDocument(
        @Parameter(description = "ContentType 열거값 이름 (예: INTRO)", required = true)
        @RequestParam contentType: String,
    ): Rest<ContentDocumentResponse> {
        return Rest.ok(contentService.getDocumentByType(ContentType.valueOf(contentType)))
    }

    @AdminRequired
    @Operation(
        summary = "콘텐츠 문서 저장",
        description = "콘텐츠 문서를 추가하거나 수정한다. 로그인 세션 또는 AccessToken이 필요하다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    @PutMapping
    fun upsertContentDocument(
        @Parameter(description = "콘텐츠 문서 저장 요청 데이터", required = true)
        @RequestBody request: ContentDocumentRequest,
    ): Rest<ContentDocumentResponse> {
        return Rest.ok(contentService.upsertDocument(request))
    }
}
