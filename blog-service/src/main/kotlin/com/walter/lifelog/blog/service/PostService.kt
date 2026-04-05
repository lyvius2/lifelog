package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.dto.PostSimpleInfo
import com.walter.lifelog.blog.mapper.PostMapper
import com.walter.lifelog.blog.repository.PostsQueryRepository
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.config.exception.PostNotFoundException
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.MarkdownConverter
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postsQueryRepository: PostsQueryRepository,
    private val postMapper: PostMapper,
    private val cacheManager: CacheManager,
) {
    @DynamicCacheable(value = ["post"], key = "#postSeq", ttlMinutes = 1440)
    fun getPost(postSeq: Long): PostResponse {
        val post = postsRepository.findByPostSeq(postSeq) ?: throw PostNotFoundException(postSeq.toString())
        return postMapper.toDto(post)
    }

    fun getPost(slug: String): PostResponse {
        val post = postsRepository.findBySlug(slug) ?: throw PostNotFoundException(slug)
        return postMapper.toDto(post)
    }

    @DynamicCacheable(value = ["prevPostInfo"], key = "#post.postSeq", ttlMinutes = 5)
    fun getPrevPostInfo(post: PostResponse): PostSimpleInfo? {
        return postsRepository.findPrevPost(post.categorySeq, post.createdAt)?.let { postMapper.toPostSimpleInfoDto(it) }
    }

    @DynamicCacheable(value = ["nextPostInfo"], key = "#post.postSeq", ttlMinutes = 5)
    fun getNextPostInfo(post: PostResponse): PostSimpleInfo? {
        return postsRepository.findNextPost(post.categorySeq, post.createdAt)?.let { postMapper.toPostSimpleInfoDto(it) }
    }

    fun getSearchedPosts(postSearchCondition: PostSearchCondition): PageResponse<PostListResponse> {
        return postsQueryRepository.findSearchedPosts(postSearchCondition)
    }

    fun savePost(postRequest: PostRequest, userSeq: Long) : PostResponse {
        val content = MarkdownConverter.convert(postRequest.markdownContent)
        postRequest.apply {
            this.content = content
            this.userSeq = userSeq
        }
        val postEntity = postMapper.toEntity(postRequest)
        if (postRequest.postSeq != null) {
            postsRepository.findByPostSeq(postRequest.postSeq)?.let { postEntity.viewCount = it.viewCount }
            cacheManager.getCache("post")?.evict(postRequest.postSeq)
        }
        return postMapper.toDto(postsRepository.save(postEntity))
    }
}