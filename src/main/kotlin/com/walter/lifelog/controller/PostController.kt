package com.walter.lifelog.controller

import com.walter.lifelog.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "게시글", description = "블로그 게시글 관리 API")
@RequestMapping("/api/post")
@RestController
class PostController(
    private val postService: PostService,
) {
    @Operation(
        summary = "게시글 조회",
        description = "게시글 Seq 또는 slug로 게시글을 조회합니다."
    )
    @GetMapping("/{inquiryStr}")
    fun getPost(
        @Parameter(description = "게시글 Seq 또는 slug", required = true)
        @PathVariable inquiryStr: String
    ) = postService.getPost(inquiryStr)
}