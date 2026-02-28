package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.CategoriesRepository
import com.walter.lifelog.repository.PostsRepository
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
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
}