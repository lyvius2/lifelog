package com.walter.lifelog.service

import com.walter.lifelog.config.exception.PostNotFoundException
import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.controller.dto.PostSaveResponse
import com.walter.lifelog.entity.PostTag
import com.walter.lifelog.entity.code.PostStatus
import com.walter.lifelog.mapper.CategoryMapper
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.repository.CategoriesRepository
import com.walter.lifelog.repository.PostTagsRepository
import com.walter.lifelog.repository.PostsRepository
import com.walter.lifelog.util.MarkdownConverter
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
    private val postTagsRepository: PostTagsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val categoryMapper: CategoryMapper,
) {
    @Transactional(readOnly = true)
    fun getPost(inquiryStr: String): PostResponse {
        val post = if (inquiryStr.toLongOrNull() != null) {
            postsRepository.findByPostSeq(inquiryStr.toLong())
        } else {
            postsRepository.findBySlug(inquiryStr)
        }
        if (post == null) {
            throw PostNotFoundException(inquiryStr)
        }
        val tags = postTagsRepository.findByPostSeq(post.postSeq!!)
        val tagList = tags.map { it.tag }
        return postMapper.toDto(post).apply {
            this.tags = tagList
        }
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
    fun savePost(postRequest: PostRequest, userSeq: Long) : PostSaveResponse {
        val content = MarkdownConverter.convert(postRequest.markdownContent)
        postRequest.apply {
            this.content = content
            this.userSeq = userSeq
        }
        val postEntity = postMapper.toEntity(postRequest)
        if (postRequest.status == PostStatus.PUBLISHED.name) {
            postEntity.publishedAt = LocalDateTime.now()
        }
        val post = postsRepository.save(postEntity)

        postRequest.postSeq?.let { postTagsRepository.deleteByPostSeq(it) }
        var index = 0
        postRequest.tags?.forEach { tag ->
            val postTag = PostTag(postSeq = post.postSeq!!, tagSeq = index++, tag = tag)
            postTagsRepository.save(postTag)
        }
        return PostSaveResponse(postSeq = post.postSeq!!, title = post.title)
    }
}