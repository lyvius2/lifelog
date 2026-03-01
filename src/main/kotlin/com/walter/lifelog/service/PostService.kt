package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.entity.PostTag
import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.CategoriesRepository
import com.walter.lifelog.repository.PostTagsRepository
import com.walter.lifelog.repository.PostsRepository
import com.walter.lifelog.util.MarkdownConverter
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
    private val postTagsRepository: PostTagsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val categoryMapper: CategoryMapper,
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

    fun getEditPost() : Map<String, Any> {
        val categories = categoriesRepository.findInActive()
        return mapOf(
            "categories" to categoryMapper.toPostInputCategoryList(categories),
            "content" to PostResponse(title = StringUtils.EMPTY, content = StringUtils.EMPTY)
        )
    }

    fun getEditPost(postSeq: Long) : Map<String, Any> {
        val post = getPost(postSeq.toString())
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

    @Transactional
    fun savePost(postRequest: PostRequest, userSeq: Long) {
        val content = MarkdownConverter.convert(postRequest.markdownContent)
        postRequest.apply {
            this.content = content
            this.userSeq = userSeq
        }
        val postEntity = postMapper.toEntity(postRequest)
        val post = postsRepository.save(postEntity)
        var index = 0
        postRequest.tags?.forEach { tag ->
            val postTag = PostTag(postSeq = post.postSeq!!, tagSeq = index++, tag = tag)
            postTagsRepository.save(postTag)
        }
    }
}