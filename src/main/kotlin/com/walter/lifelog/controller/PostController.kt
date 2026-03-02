package com.walter.lifelog.controller

import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.controller.dto.PostSaveResponse
import com.walter.lifelog.controller.dto.Rest
import com.walter.lifelog.facade.PostFacade
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
import org.springframework.web.bind.annotation.RequestHeader

@Tag(name = "게시글", description = "블로그 게시글 관리 API")
@RequestMapping("/api/post")
@RestController
class PostController(
    private val postFacade: PostFacade,
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
        summary = "게시글 저장",
        description = "새 게시글을 저장합니다. 로그인 세션이 필요합니다."
    )
    @PostMapping("/save")
    fun savePost(@RequestBody postRequest: PostRequest,
                 @RequestHeader("Authorization") authorization: String?,
                 session: HttpSession?) : Rest<PostSaveResponse> {
        val userSeq = postFacade.validateAuthor(authorization, session)
        return Rest.ok(postFacade.savePost(postRequest, userSeq))
    }
}