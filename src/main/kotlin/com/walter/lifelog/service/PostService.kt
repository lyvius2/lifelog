package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.entity.Post
import com.walter.lifelog.entity.code.PostStatus
import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.CategoriesRepository
import com.walter.lifelog.repository.PostTagsRepository
import com.walter.lifelog.repository.PostsRepository
import com.walter.lifelog.util.MarkdownConverter
import org.apache.commons.lang3.StringUtils
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

    fun getEditPost() : Map<String, Any> {
        val categories = categoriesRepository.findInActive()
        return mapOf(
            "categories" to categoryMapper.toPostInputCategoryList(categories),
            "content" to PostResponse(title = StringUtils.EMPTY, content = StringUtils.EMPTY)
        )
    }

    fun getEditPost(postSeq: Long) : Map<String, Any> {
        val postEntity = getPost(postSeq.toString())
        val tags = postTagsRepository.findByPostSeq(postEntity.postSeq!!)
        val tagList = tags.map { it.tag }
        val post = postMapper.toDto(postEntity).apply {
            this.tags = tagList
        }

        val postCategorySeq = post.categorySeq
        val categories = categoryMapper.toPostInputCategoryList(
            categoriesRepository.findInActive()
        )
        categories.filter { it.categorySeq == postCategorySeq }
            .map { it.isChecked = true }
        return mapOf(
            "categories" to categories,
            "content" to post
        )
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