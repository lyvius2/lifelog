package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.blog.dto.PageResponse
import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.facade.PostFacade
import com.walter.lifelog.shared.util.AccessTokenHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestHeader

@Tag(name = "게시글", description = "블로그 게시글 관리 API")
@RequestMapping("/api/post")
@RestController
class PostController(
    private val postFacade: PostFacade,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    @Operation(
        summary = "게시글 조회",
        description = "게시글 Seq 또는 slug로 게시글을 조회합니다."
    )
    @GetMapping("/{inquiryStr}")
    fun getPost(@Parameter(description = "게시글 Seq 또는 slug", required = true)
                @PathVariable inquiryStr: String) : Rest<PostResponse> {
        return Rest.ok(postFacade.getPost(inquiryStr))
    }

    @Operation(
        summary = "게시글 검색",
        description = "검색 조건에 따라 게시글 목록을 페이징하여 조회합니다."
    )
    @PostMapping("/search")
    fun getSearchedPosts(@Parameter(description = "검색 조건", required = false)
                         @RequestBody postSearchCondition: PostSearchCondition?) : Rest<PageResponse<PostListResponse>> {
        return Rest.ok(postFacade.getSearchedPosts(postSearchCondition))
    }

    @Operation(
        summary = "게시글 저장",
        description = "새 게시글을 저장합니다. 로그인 세션이 필요합니다."
    )
    @PostMapping("/save")
    fun savePost(@Parameter(description = "게시글 저장 요청 데이터", required = true)
                 @RequestBody postRequest: PostRequest,
                 @Parameter(description = "JWT 인증 토큰", required = false)
                 @RequestHeader("Authorization") authorization: String?,
                 @Parameter(hidden = true) session: HttpSession?) : Rest<PostSaveResponse> {
        val userSeq = if (authorization != null) {
            AccessTokenHandler.getUserSeqFromToken(authorization, jwtSecretKey)
        } else {
            session!!.getAttribute("userSeq") as? Long ?: throw IllegalStateException("로그인이 필요합니다.")
        }
        return Rest.ok(postFacade.savePost(postRequest, userSeq))
    }
}