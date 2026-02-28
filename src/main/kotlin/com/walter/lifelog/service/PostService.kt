package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.PostsRepository
import org.springframework.stereotype.Service

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
) {
    fun getPost(inquiryStr: String): PostResponse {
        val post = if (inquiryStr.toLongOrNull() != null) {
            postsRepository.findByPostSeq(inquiryStr.toLong())
        } else {
            postsRepository.findBySlug(inquiryStr)
        }

        return postMapper.toDto(
            post ?: throw PostNotFoundException(inquiryStr)
        )
    }
}