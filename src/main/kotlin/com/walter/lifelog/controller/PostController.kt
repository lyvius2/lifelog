package com.walter.lifelog.controller

import com.walter.lifelog.service.PostService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/post")
@RestController
class PostController(
    private val postService: PostService,
) {
    @GetMapping("/{inquiryStr}")
    fun getPost(@PathVariable inquiryStr: String) = postService.getPost(inquiryStr)
}