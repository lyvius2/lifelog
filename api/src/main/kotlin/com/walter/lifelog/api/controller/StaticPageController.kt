package com.walter.lifelog.api.controller

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.content.dto.StaticPageScrapeRequest
import com.walter.lifelog.content.facade.StaticPageFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "정적 웹페이지", description = "정적 웹페이지 등록 및 관리 API")
@RequestMapping("/api/static-page")
@RestController
class StaticPageController(
    private val staticPageFacade: StaticPageFacade,
) {
    @AdminRequired
    @PostMapping("/upload", consumes = ["multipart/form-data"])
    @Operation(
        summary = "HTML 파일로 정적 웹페이지 등록",
        description = "HTML 파일을 올려 /static/{path} 로 서비스합니다. 확장자·크기·문서 골격을 검사해 " +
            "요건에 맞지 않으면 거부합니다. path 를 비워 두면 소문자와 숫자를 섞어 8자로 자동 생성합니다. " +
            "path 가 이미 쓰이고 있으면 409 와 함께 기존 페이지 정보를 돌려주며, overwrite=true 로 다시 부르면 덮어씁니다. " +
            "isPublic=false 로 올리면 로그인한 관리자에게만 보입니다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    fun upload(
        @Parameter(description = "HTML 파일", required = true)
        @RequestPart("file") file: MultipartFile,
        @Parameter(description = "원하는 접속 경로 (비워 두면 자동 생성)", required = false, example = "my-dashboard")
        @RequestParam("path", required = false) path: String?,
        @Parameter(description = "경로가 이미 쓰이고 있을 때 덮어쓸지 여부", required = false, example = "false")
        @RequestParam("overwrite", required = false, defaultValue = "false") overwrite: Boolean,
        @Parameter(description = "링크가 있으면 누구나 볼 수 있게 할지 여부", required = false, example = "true")
        @RequestParam("isPublic", required = false, defaultValue = "true") isPublic: Boolean,
        @Parameter(hidden = true) request: HttpServletRequest,
    ): Rest<StaticPageResponse> {
        val userSeq = request.getAttribute("userSeq") as Long
        return Rest.ok(
            staticPageFacade.registerFromFile(
                path, file.originalFilename, file.bytes, userSeq, overwrite, isPublic
            )
        )
    }

    @AdminRequired
    @PostMapping("/scrape")
    @Operation(
        summary = "Web URL 로 정적 웹페이지 등록",
        description = "주소를 스크랩해 HTML 마크업을 내려받아 /static/{path} 로 서비스합니다. " +
            "HTTP 200 이 아니거나 HTML 이 아니면 거부하며, 내부망 주소는 등록할 수 없습니다. " +
            "path 가 이미 쓰이고 있으면 409 와 함께 기존 페이지 정보를 돌려주며, overwrite=true 로 다시 부르면 덮어씁니다. " +
            "isPublic=false 로 올리면 로그인한 관리자에게만 보입니다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    fun scrape(
        @Parameter(description = "스크랩 요청 데이터", required = true)
        @RequestBody scrapeRequest: StaticPageScrapeRequest,
        @Parameter(hidden = true) request: HttpServletRequest,
    ): Rest<StaticPageResponse> {
        val userSeq = request.getAttribute("userSeq") as Long
        return Rest.ok(
            staticPageFacade.registerFromUrl(
                scrapeRequest.path, scrapeRequest.url, userSeq, scrapeRequest.overwrite, scrapeRequest.isPublic
            )
        )
    }

    @AdminRequired
    @GetMapping("")
    @Operation(
        summary = "정적 웹페이지 목록 조회",
        description = "등록된 정적 웹페이지를 최신순으로 조회합니다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    fun getPages(): Rest<List<StaticPageResponse>> {
        return Rest.ok(staticPageFacade.getPages())
    }

    @AdminRequired
    @DeleteMapping("/delete")
    @Operation(
        summary = "정적 웹페이지 삭제",
        description = "정적 웹페이지를 논리 삭제합니다 (is_active = false). 삭제한 경로는 재사용하지 않습니다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    fun delete(
        @Parameter(description = "접속 경로", required = true, example = "my-dashboard")
        @RequestParam("path") path: String,
    ): Rest<Unit> {
        staticPageFacade.delete(path)
        return Rest.ok(Unit)
    }
}
