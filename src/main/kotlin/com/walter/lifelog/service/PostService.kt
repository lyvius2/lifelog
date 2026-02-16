package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.entity.Post
import com.walter.lifelog.repository.PostsRepository
import org.springframework.stereotype.Service

@Service
class PostService(
    private val postsRepository: PostsRepository,
) {
    fun getPost(inquiryStr: String): Post {
        val post = if (inquiryStr.toLongOrNull() != null) {
            postsRepository.findByPostSeq(inquiryStr.toLong())
        } else {
            postsRepository.findBySlug(inquiryStr)
        }

        return post ?: throw PostNotFoundException(inquiryStr)
    }
}