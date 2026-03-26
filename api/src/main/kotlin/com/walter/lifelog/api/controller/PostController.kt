package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.CreateSummaryRequest
import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.blog.dto.CategoryTreeResponse
import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.facade.PostFacade
import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.paging.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@Tag(name = "게시글", description = "블로그 게시글 관리 API")
@RequestMapping("/api/post")
@RestController
class PostController(
    private val postFacade: PostFacade,
) {
    @Operation(summary = "게시글 조회", description = "게시글 Seq 또는 slug로 게시글을 조회한다.")
    @GetMapping("/{inquiryStr}")
    fun getPost(@Parameter(description = "게시글 Seq 또는 slug", required = true)
                @PathVariable inquiryStr: String) : Rest<PostResponse> {
        return Rest.ok(postFacade.getPost(inquiryStr))
    }

    @Operation(summary = "게시글 검색", description = "검색 조건에 따라 게시글 목록을 페이징하여 조회한다.")
    @PostMapping("/search")
    fun getSearchedPosts(@Parameter(description = "검색 조건", required = false)
                         @RequestBody postSearchCondition: PostSearchCondition?) : Rest<PageResponse<PostListResponse>> {
        return Rest.ok(postFacade.getSearchedPosts(postSearchCondition))
    }

    @AdminRequired
    @Operation(summary = "게시글 저장", description = "새 게시글을 저장한다. 로그인 세션 또는 AccessToken이 필요.", security = [SecurityRequirement(name = "Authorization")])
    @PostMapping("/save")
    fun savePost(@Parameter(description = "게시글 저장 요청 데이터", required = true)
                 @RequestBody postRequest: PostRequest,
                 @Parameter(hidden = true) request: HttpServletRequest) : Rest<PostSaveResponse> {
        val userSeq = request.getAttribute("userSeq") as Long
        return Rest.ok(postFacade.savePost(postRequest, userSeq))
    }

    @Operation(summary = "카테고리 트리 조회", description = "활성화된 카테고리를 최대 3 depth 트리 구조로 조회합니다.")
    @GetMapping("/category/tree")
    fun getCategoryTree(): Rest<List<CategoryTreeResponse>> {
        return Rest.ok(postFacade.getCategoryTree())
    }

    @AdminRequired
    @Operation(summary = "AI 자동 요약 생성", description = "게시글 본문을 기반으로 AI가 요약을 생성한다. 로그인 세션 또는 AccessToken이 필요.", security = [SecurityRequirement(name = "Authorization")])
    @PostMapping("/create-summary")
    fun createAutoSummary(@Parameter(description = "요약 생성 요청 데이터", required = true)
                          @RequestBody createSummaryRequest: CreateSummaryRequest) : Rest<String> {
        return Rest.ok(postFacade.getCreatedSummary(createSummaryRequest.content))
    }
}