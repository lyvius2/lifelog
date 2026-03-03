package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.entity.Post
import com.walter.lifelog.entity.code.PostStatus
import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.CategoriesRepository
import com.walter.lifelog.repository.PostTagsRepository
import com.walter.lifelog.repository.PostsRepository
import com.walter.lifelog.util.MarkdownConverter
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
    private val postTagsRepository: PostTagsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val categoryMapper: CategoryMapper,
) {
    fun getPost(inquiryStr: String): Post {
        val post = if (inquiryStr.toLongOrNull() != null) {
            postsRepository.findByPostSeq(inquiryStr.toLong())
        } else {
            postsRepository.findBySlug(inquiryStr)
        }
        if (post == null) {
            throw PostNotFoundException(inquiryStr)
        }
        return post
    }

    fun getPreviousPost(categorySeq: Long, createdAt: LocalDateTime): Post? {
        return postsRepository.findPrevPost(categorySeq, createdAt)
    }

    fun getNextPost(categorySeq: Long, createdAt: LocalDateTime): Post? {
        return postsRepository.findNextPost(categorySeq, createdAt)
    }

    fun savePost(postRequest: PostRequest, userSeq: Long) : Post {
        val content = MarkdownConverter.convert(postRequest.markdownContent)
        postRequest.apply {
            this.content = content
            this.userSeq = userSeq
        }
        val postEntity = postMapper.toEntity(postRequest)
        if (postRequest.status == PostStatus.PUBLISHED.name) {
            postEntity.publishedAt = LocalDateTime.now()
        }
        return postsRepository.save(postEntity)
    }
}